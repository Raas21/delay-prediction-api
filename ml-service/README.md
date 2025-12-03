# ML Service for Delay Prediction

This service provides a delay prediction API for the NYC bus system.

## Training the Model

To train the model, run the following command from the root of the project:

```bash
docker-compose run --build train-model
```

This will run the training script in `src/models/train_model.py` and save the trained model to the `ml_models` volume.

## Prediction API

The prediction API is available at `http://localhost:8000`.

### `GET /predict/{route_id}/{vehicle_id}`

Predicts the delay for a given vehicle on a given route.

**Example:**

```bash
curl http://localhost:8000/predict/B46/1234
```

**Response:**

```json
{
  "route_id": "B46",
  "vehicle_id": "1234",
  "timestamp": "2025-01-01T12:34:56Z",
  "predicted_delay_minutes": 4.5,
  "features_used": {
    "hour_of_day": 12,
    "day_of_week": 3,
    "stop_id": "123456",
    "direction_id": 1
  },
  "model_version": "v1"
}
```

### `GET /train`

Triggers the model training process.

**Example:**

```bash
curl http://localhost:8000/train
```

**Response:**

```json
{
  "message": "Model trained successfully."
}
```
