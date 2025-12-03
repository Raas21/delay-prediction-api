import pandas as pd
import joblib
from sklearn.ensemble import RandomForestRegressor
from sklearn.preprocessing import LabelEncoder
from datetime import datetime
import os
import logging
from src.data.fetch_data import DataFetcher

logging.basicConfig(level=logging.INFO)

class DelayModel:
    def __init__(self):
        self.model = RandomForestRegressor(n_estimators=100, random_state=42)
        self.encoders = {}

    def preprocess(self, df, training=True):
        """Preprocess data for training or prediction."""
        df = df.dropna(subset=['route_id', 'stop_id', 'delay'])
        
        # Convert timestamp to datetime if it's a string
        if isinstance(df['timestamp'].iloc[0], str):
            df['timestamp'] = pd.to_datetime(df['timestamp'])

        df['hour'] = df['timestamp'].dt.hour
        df['day_of_week'] = df['timestamp'].dt.weekday

        categorical_features = ['route_id', 'stop_id']
        for col in categorical_features:
            if training:
                le = LabelEncoder()
                df[f'{col}_encoded'] = le.fit_transform(df[col])
                self.encoders[col] = le
            else:
                le = self.encoders.get(col)
                if le:
                    # Handle unseen labels
                    df[f'{col}_encoded'] = df[col].apply(lambda x: le.transform([x])[0] if x in le.classes_ else -1)
                else:
                    raise ValueError(f"Encoder for {col} not found.")

        features = [f'{col}_encoded' for col in categorical_features] + ['hour', 'day_of_week', 'latitude', 'longitude']
        return df[features], df['delay']

    def train(self, df):
        """Train the Random Forest model."""
        X, y = self.preprocess(df)
        if len(X) == 0:
            logging.warning("No valid data for training")
            return
        self.model.fit(X, y)
        logging.info("Model training complete.")

    def predict(self, data):
        """Predict delay for a single data point."""
        df = pd.DataFrame([data])
        X, _ = self.preprocess(df, training=False)
        return self.model.predict(X)[0]

    def save(self, path):
        """Save the model and encoders."""
        os.makedirs(os.path.dirname(path), exist_ok=True)
        joblib.dump({'model': self.model, 'encoders': self.encoders}, path)
        logging.info(f"Model saved to {path}")

    def load(self, path):
        """Load the model and encoders."""
        data = joblib.load(path)
        self.model = data['model']
        self.encoders = data['encoders']
        logging.info(f"Model loaded from {path}")


if __name__ == "__main__":
    fetcher = DataFetcher()
    df = fetcher.fetch_historical_data()
    if not df.empty:
        model = DelayModel()
        model.train(df)
        model.save('/app/models/delay_model.joblib')