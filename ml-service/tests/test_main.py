from fastapi.testclient import TestClient
from main import app
import pytest
from unittest.mock import patch, MagicMock
import os
import pandas as pd

client = TestClient(app)

@pytest.fixture(scope="module")
def mock_redis():
    with patch('redis.Redis') as mock_redis_class:
        mock_redis_instance = MagicMock()
        mock_redis_class.return_value = mock_redis_instance
        yield mock_redis_instance

@pytest.fixture(scope="module")
def trained_model():
    with patch('main.model') as mock_model:
        mock_model.encoders = {'route_id': MagicMock(), 'stop_id': MagicMock()}
        mock_model.predict.return_value = 300
        yield mock_model

def test_predict_delay_success(mock_redis, trained_model):
    mock_redis.get.return_value = '{"routeId": "B46", "stopId": "123", "timestamp": {"epochSecond": 1672531200}}'
    response = client.get("/predict/B46/1234")
    assert response.status_code == 200
    json_response = response.json()
    assert json_response['route_id'] == 'B46'
    assert json_response['vehicle_id'] == '1234'
    assert 'predicted_delay_minutes' in json_response

def test_predict_delay_no_data(mock_redis, trained_model):
    mock_redis.get.return_value = None
    response = client.get("/predict/B46/1234")
    assert response.status_code == 404

def test_predict_delay_model_not_trained(mock_redis):
    with patch('main.model') as mock_model:
        mock_model.encoders = {}
        response = client.get("/predict/B46/1234")
        assert response.status_code == 503

@patch('main.DataFetcher')
@patch('main.DelayModel')
def test_train_model_endpoint(MockDelayModel, MockDataFetcher):
    mock_fetcher = MockDataFetcher.return_value
    mock_fetcher.fetch_historical_data.return_value = pd.DataFrame({'route_id': ['B46'], 'stop_id': ['123'], 'delay': [120], 'timestamp': [pd.Timestamp.now()], 'latitude': [0], 'longitude': [0]})
    
    mock_model_instance = MockDelayModel.return_value
    
    response = client.get("/train")
    
    assert response.status_code == 200
    assert response.json() == {"message": "Model trained successfully."}
    mock_model_instance.train.assert_called_once()
    mock_model_instance.save.assert_called_once()

@patch('main.DataFetcher')
def test_train_model_endpoint_no_data(MockDataFetcher):
    mock_fetcher = MockDataFetcher.return_value
    mock_fetcher.fetch_historical_data.return_value = pd.DataFrame()
    
    response = client.get("/train")
    
    assert response.status_code == 500

