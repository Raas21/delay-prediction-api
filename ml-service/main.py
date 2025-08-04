from fastapi import FastAPI, HTTPException
from src.data.fetch_data import DataFetcher
from src.models.train_model import DelayModel
import os

app = FastAPI(title="NYC Bus Delay Prediction")

fetcher = DataFetcher()
model = DelayModel()

@app.on_event("startup")
def startup_event():
    """Train model on startup if historical data exists."""
    df = fetcher.fetch_historical_data()
    if not df.empty:
        model.train(df)

@app.get("/predict/{route_id}/{vehicle_id}")
async def predict_delay(route_id: str, vehicle_id: str):
    """Predict delay for a vehicle on a route."""
    if not route_id.startswith("B"):
        raise HTTPException(status_code=400, detail="Only Brooklyn routes (starting with 'B') supported")
    
    data = fetcher.fetch_realtime_data(vehicle_id)
    if not data or data['route_id'] != route_id:
        raise HTTPException(status_code=404, detail=f"No real-time data for vehicle {vehicle_id} on route {route_id}")
    
    try:
        delay = model.predict(data)
        return {"route_id": route_id, "vehicle_id": vehicle_id, "predicted_delay": int(delay)}
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Prediction error: {str(e)}")