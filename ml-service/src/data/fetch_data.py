import pandas as pd
import psycopg2
from dotenv import load_dotenv
import os
import json

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