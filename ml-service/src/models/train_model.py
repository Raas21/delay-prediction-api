import pandas as pd
import pickle
from sklearn.ensemble import RandomForestRegressor
from sklearn.preprocessing import LabelEncoder
from datetime import datetime
import os

class DelayModel:
    def __init__(self):
        self.model = RandomForestRegressor(n_estimators=100, random_state=42)
        self.route_encoder = LabelEncoder()
        self.stop_encoder = LabelEncoder()

    def preprocess(self, df):
        """Preprocess data for training."""
        df = df.dropna(subset=['route_id', 'stop_id', 'delay'])
        df['hour'] = df['timestamp'].apply(lambda x: x.hour)
        df['day_of_week'] = df['timestamp'].apply(lambda x: x.weekday())
        df['route_encoded'] = self.route_encoder.fit_transform(df['route_id'])
        df['stop_encoded'] = self.stop_encoder.fit_transform(df['stop_id'])
        features = ['route_encoded', 'stop_encoded', 'hour', 'day_of_week', 'latitude', 'longitude']
        return df[features], df['delay']

    def train(self, df):
        """Train the Random Forest model."""
        X, y = self.preprocess(df)
        if len(X) == 0:
            print("No valid data for training")
            return
        self.model.fit(X, y)
        with open('/app/src/models/model.pkl', 'wb') as f:
            pickle.dump({'model': self.model, 'route_encoder': self.route_encoder, 'stop_encoder': self.stop_encoder}, f)
        print("Model trained and saved to /app/src/models/model.pkl")

    def predict(self, data):
        """Predict delay for a single data point."""
        if not os.path.exists('/app/src/models/model.pkl'):
            raise ValueError("Model file not found")
        with open('/app/src/models/model.pkl', 'rb') as f:
            saved = pickle.load(f)
            model = saved['model']
            route_encoder = saved['route_encoder']
            stop_encoder = saved['stop_encoder']
        
        df = pd.DataFrame([data])
        df['hour'] = df['timestamp'].apply(lambda x: datetime.fromisoformat(x).hour)
        df['day_of_week'] = df['timestamp'].apply(lambda x: datetime.fromisoformat(x).weekday())
        df['route_encoded'] = route_encoder.transform([df['route_id'].iloc[0]])[0]
        df['stop_encoded'] = stop_encoder.transform([df['stop_id'].iloc[0]])[0]
        features = ['route_encoded', 'stop_encoded', 'hour', 'day_of_week', 'latitude', 'longitude']
        return model.predict(df[features])[0]