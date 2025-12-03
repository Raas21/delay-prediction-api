from fastapi import FastAPI, HTTPException
from src.data.fetch_data import DataFetcher
from src.models.train_model import DelayModel
import os
import asyncio
import redis
import json
from datetime import datetime
from kafka import KafkaConsumer

app = FastAPI(title="NYC Bus Delay Prediction")

model = DelayModel()
redis_client = redis.Redis(host=os.getenv('REDIS_HOST', 'localhost'), port=6379, db=0)

def get_kafka_consumer():
    return KafkaConsumer(
        'vehicle_positions',
        bootstrap_servers=os.getenv('KAFKA_BOOTSTRAP_SERVERS', 'kafka:9092'),
        value_deserializer=lambda x: json.loads(x.decode('utf-8')),
        auto_offset_reset='latest',
        group_id='ml-service'
    )

@app.on_event("startup")
async def startup_event():
    """Load the model and start the Kafka consumer."""
    model_path = '/app/models/delay_model.joblib'
    if os.path.exists(model_path):
        model.load(model_path)
    else:
        # This is a fallback for when the model is not trained yet.
        # The service will not be able to make predictions until the model is trained.
        print("Model not found. Please train the model first.")

    asyncio.create_task(consume_kafka_messages())

async def consume_kafka_messages():
    """Consume messages from Kafka and store them in Redis."""
    consumer = get_kafka_consumer()
    for message in consumer:
        data = message.value
        vehicle_id = data.get('vehicleId')
        if vehicle_id:
            redis_client.set(f"vehicle:{vehicle_id}", json.dumps(data))

@app.get("/predict/{route_id}/{vehicle_id}")
async def predict_delay(route_id: str, vehicle_id: str):
    """Predict delay for a vehicle on a route."""
    if not model.encoders:
        raise HTTPException(status_code=503, detail="Model not trained yet.")

    data_str = redis_client.get(f"vehicle:{vehicle_id}")
    if not data_str:
        raise HTTPException(status_code=404, detail=f"No real-time data for vehicle {vehicle_id}")

    data = json.loads(data_str)
    
    # The data from kafka is camelCase, but the model expects snake_case
    data['route_id'] = data.pop('routeId')
    data['stop_id'] = data.pop('stopId')


    if data['route_id'] != route_id:
        raise HTTPException(status_code=404, detail=f"Vehicle {vehicle_id} is not on route {route_id}")

    try:
        # The timestamp from the kafka message is a dict with epochSecond and nano fields.
        data['timestamp'] = datetime.fromtimestamp(data['timestamp']['epochSecond'])
        
        prediction = model.predict(data)
        
        # Prepare the feature vector used for the prediction
        features_used = {
            "hour_of_day": data['timestamp'].hour,
            "day_of_week": data['timestamp'].weekday(),
            "stop_id": data['stop_id'],
            "direction_id": data.get("directionId"), # This might not be available
        }
        
        return {
            "route_id": route_id,
            "vehicle_id": vehicle_id,
            "timestamp": data['timestamp'].isoformat(),
            "predicted_delay_minutes": round(prediction / 60, 2),
            "features_used": features_used,
            "model_version": "v1" 
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Prediction error: {str(e)}")

@app.get("/train")
async def train_model_endpoint():
    """Train the model from historical data."""
    fetcher = DataFetcher()
    df = fetcher.fetch_historical_data()
    if not df.empty:
        global model
        model = DelayModel()
        model.train(df)
        model.save('/app/models/delay_model.joblib')
        return {"message": "Model trained successfully."}
    else:
        raise HTTPException(status_code=500, detail="Could not fetch historical data.")
