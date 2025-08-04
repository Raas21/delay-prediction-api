import pandas as pd
import psycopg2
from kafka import KafkaConsumer
from dotenv import load_dotenv
import os
import json
from datetime import datetime

load_dotenv()

class DataFetcher:
    def __init__(self):
        self.db_params = {
            'dbname': os.getenv('DB_NAME', 'postgres'),
            'user': os.getenv('DB_USER', 'postgres'),
            'password': os.getenv('DB_PASSWORD', 'password'),
            'host': os.getenv('DB_HOST', 'postgres'),
            'port': os.getenv('DB_PORT', '5432')
        }
        self.consumer = KafkaConsumer(
            'vehicle_positions',
            bootstrap_servers=os.getenv('KAFKA_BOOTSTRAP_SERVERS', 'kafka:9092'),
            value_deserializer=lambda x: json.loads(x.decode('utf-8')),
            auto_offset_reset='latest',
            group_id='ml-service'
        )

    def fetch_historical_data(self):
        """Fetch historical VehiclePosition data from PostgreSQL."""
        try:
            conn = psycopg2.connect(**self.db_params)
            query = """
                SELECT vehicle_id, route_id, stop_id, latitude, longitude, timestamp, delay
                FROM vehicle_position
                WHERE route_id LIKE 'B%'
            """
            df = pd.read_sql(query, conn)
            conn.close()
            return df
        except Exception as e:
            print(f"Error fetching historical data: {e}")
            return pd.DataFrame()

    def fetch_realtime_data(self, vehicle_id):
        """Fetch real-time VehiclePosition data from Kafka."""
        try:
            for message in self.consumer:
                data = message.value
                if data['vehicle_id'] == vehicle_id:
                    return data
            return None
        except Exception as e:
            print(f"Error fetching real-time data for {vehicle_id}: {e}")
            return None