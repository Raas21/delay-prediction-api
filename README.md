# Real-Time Public Transit Delay Prediction API

This project delivers a real-time API for predicting bus delays within the New York City public transit system, specifically focusing on Brooklyn routes. It integrates real-time vehicle position data from the MTA GTFS-RT feed, processes it to calculate observed delays, and leverages a machine learning model to forecast future delays. The entire system is containerized using Docker for easy deployment and management.

## Project Overview

The core objective is to provide timely and accurate bus delay predictions, enhancing the rider experience. The system ingests live vehicle data, stores historical information, and continuously updates a predictive model to offer up-to-the-minute delay forecasts via a dedicated API endpoint.

## Architecture and Data Flow

The solution is built upon a microservices architecture, orchestrated with Docker Compose, involving both Java and Python services interacting with various data stores and a streaming platform.

1.  **MTA GTFS-RT Feed Ingestion (Java App Service):**
    *   The **Java App Service**, built with Spring Boot, acts as the primary data ingress point.
    *   It periodically fetches real-time vehicle position updates from the official MTA GTFS-RT (General Transit Feed Specification - Realtime) API.
    *   Upon ingestion, it filters these updates to include only vehicles operating on Brooklyn routes (identified by `route_id` starting with "B").
    *   For each relevant vehicle, it computes the observed delay by comparing actual vehicle timestamps against scheduled times.
    *   These processed vehicle position records are then persisted in a **PostgreSQL** database for historical analysis and model training, and a subset (e.g., the latest positions) is cached in **Redis** for quick retrieval by the prediction service.
    *   Crucially, these real-time, processed vehicle positions are also published to a **Kafka** topic named `vehicle_positions` as JSON messages, serving as a streaming backbone for real-time consumers.

2.  **Real-time Data Processing & Prediction (Python ML Service):**
    *   The **Python ML Service**, implemented with FastAPI, serves two main functions: consuming real-time data and exposing delay predictions.
    *   It includes a background Kafka consumer that subscribes to the `vehicle_positions` topic.
    *   Messages consumed from Kafka are deserialized and immediately stored in **Redis** (keyed by `vehicle_id`) to maintain an up-to-date cache of the latest known position and status for each active bus. This ensures predictions are based on the freshest possible data.
    *   The service also exposes a `/predict/{route_id}/{vehicle_id}` API endpoint. When queried, this endpoint retrieves the latest vehicle data from Redis, constructs a feature vector, and uses a pre-trained machine learning model to predict the bus's current delay in minutes.

3.  **Model Training and Persistence:**
    *   The prediction model is developed using Python's `scikit-learn` library (e.g., RandomForestRegressor).
    *   Model training is performed offline using historical vehicle position data retrieved from the **PostgreSQL** database. Features such as `route_id`, `vehicle_id`, `stop_id`, `latitude`, `longitude`, and time-based features (hour of day, day of week) are engineered from this historical data.
    *   Once trained, the model (along with its encoders) is serialized and persisted to disk (e.g., using `joblib`) within the `ml-service` container at `/app/models/delay_model.joblib`. This persistent storage ensures the model is loaded efficiently at service startup without requiring retraining.
    *   A dedicated `train-model` Docker Compose service is provided to facilitate explicit model retraining, ensuring that the computationally intensive training process is decoupled from the live prediction service.

## Key Technologies Used

*   **Java 17 & Spring Boot:** Powers the robust and scalable data ingestion microservice.
*   **Python & FastAPI:** Provides a high-performance, asynchronous web framework for the ML prediction API and Kafka consumption.
*   **PostgreSQL:** Serves as the reliable, ACID-compliant database for long-term storage of GTFS static and real-time data.
*   **Redis:** Utilized as a high-speed cache for real-time vehicle positions, enabling low-latency predictions.
*   **Kafka:** Acts as the central nervous system for real-time data streaming between the Java ingestion service and the Python ML service.
*   **Zookeeper:** Supports Kafka for distributed coordination and metadata management.
*   **Scikit-learn & Pandas:** Fundamental Python libraries for machine learning model development, data manipulation, and feature engineering.
*   **Docker & Docker Compose:** Containerizes all services and defines their interactions, simplifying deployment and ensuring environment consistency.

## How to Run

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/Raas21/delay-prediction-api.git
    cd delay-prediction-api
    ```
2.  **Initial Data Ingestion (Important Note):**
    When running the application for the very first time, the Java app service will ingest historical GTFS-Archive data into PostgreSQL. **This process can take a significant amount of time (approximately an hour or more, depending on your PC's specifications) due to the large volume of records in the dataset.** Please be patient during this initial setup.
3.  **Build and Run the Application:**
    Ensure Docker is running, then execute:
    ```bash
    docker-compose up --build -d
    ```
    This command builds the necessary Docker images, creates the containers for all services (including PostgreSQL, Kafka, Redis, Java app, and ML service), and starts them in detached mode.

## Model Training and Continuous Learning

The predictive model requires training on historical data.

*   **Train the Model (Initial/Manual Retraining):**
    You can trigger model training in two ways:
    -   **Via API Endpoint:**
        Send a GET request to the ML service's `/train` endpoint. This is useful for on-demand retraining once the services are up:
        ```bash
        curl http://localhost:8000/train
        ```
    -   **Via Docker Compose Service:**
        Run the dedicated `train-model` service. This is ideal for initial training or script-driven retraining workflows:
        ```bash
        docker-compose run --build train-model
        ```
        This command will build the `ml-service` image (if not already built), execute the training script (`src/models/train_model.py`), and save the trained model to the `ml_models` Docker volume, making it accessible to the live `ml-service` container.

*   **Continuous Model Improvement (Future Batch Job):**
    To ensure the model remains up-to-date and incorporates the latest real-time data, a batch job will be implemented in the future. This job will run weekly, collecting the real-time vehicle position data ingested over the past week and using it to retrain the model. This continuous learning approach will help maintain the accuracy and relevance of delay predictions.

## Using the Prediction API

Once the ML service is running and a model has been trained, you can query the prediction API.

*   **Prediction Endpoint:**
    ```bash
    GET http://localhost:8000/predict/{route_id}/{vehicle_id}
    ```
    *   `route_id`: The ID of the bus route (e.g., "B46").
    *   `vehicle_id`: The unique identifier for the specific bus (e.g., "1234").

*   **Example Request:**
    ```bash
    curl http://localhost:8000/predict/B46/1234
    ```

*   **Example Success Response (JSON):**
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

*   **Error Responses:**
    *   `404 Not Found`: If no recent real-time data is available for the specified vehicle or if the vehicle is not on the given route.
    *   `503 Service Unavailable`: If the prediction model has not yet been trained or loaded.
    *   `500 Internal Server Error`: For unexpected prediction errors.

## Running Tests

To execute the unit and integration tests for the ML service, use the dedicated Docker Compose test service:

```bash
docker-compose run test
```