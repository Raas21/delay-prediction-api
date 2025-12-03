# Real-Time Public Transit Delay Prediction API

This project provides a real-time public transit delay prediction API. It ingests MTA GTFS-RT vehicle positions for NYC buses, filters for Brooklyn routes, computes a basic observed delay, and stores the records in PostgreSQL and Redis. The processed records are published to a Kafka topic, which is consumed by a Python-based ML service. The ML service provides a prediction API to predict bus delays.

## High-Level Architecture

The architecture consists of the following components:

- **Java App Service:** A Spring Boot application that ingests MTA GTFS-RT vehicle positions, processes them, and publishes them to a Kafka topic. It also stores the data in PostgreSQL and Redis.
- **Python ML Service:** A FastAPI application that consumes the Kafka topic, trains a delay prediction model, and provides a prediction API.
- **PostgreSQL:** A relational database used to store historical vehicle positions.
- **Redis:** An in-memory data store used to cache real-time vehicle positions.
- **Kafka:** A distributed streaming platform used to publish and subscribe to real-time vehicle positions.
- **Zookeeper:** A centralized service for maintaining configuration information, naming, providing distributed synchronization, and providing group services for Kafka.
- **Docker Compose:** A tool for defining and running multi-container Docker applications.

## How to Run

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/Raas21/delay-prediction-api.git
    cd delay-prediction-api
    ```
2.  **Build and run the application:**
    ```bash
    docker-compose up --build -d
    ```
3.  **Train the model:**
    You can train the model in two ways:
    -   **Using the API:**
        ```bash
        curl http://localhost:8000/train
        ```
    -   **Using docker-compose:**
        ```bash
        docker-compose run train-model
        ```
4.  **Use the prediction API:**
    ```bash
    curl http://localhost:8000/predict/{route_id}/{vehicle_id}
    ```
    For example:
    ```bash
    curl http://localhost:8000/predict/B46/1234
    ```
5.  **Run the tests:**
    ```bash
    docker-compose run test
    ```
