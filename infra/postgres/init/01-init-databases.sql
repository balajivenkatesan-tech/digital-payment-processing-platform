CREATE USER payment_service WITH PASSWORD 'payment_service';
CREATE DATABASE payment_service OWNER payment_service;
GRANT ALL PRIVILEGES ON DATABASE payment_service TO payment_service;

CREATE USER fraud_service WITH PASSWORD 'fraud_service';
CREATE DATABASE fraud_service OWNER fraud_service;
GRANT ALL PRIVILEGES ON DATABASE fraud_service TO fraud_service;

CREATE USER ledger_service WITH PASSWORD 'ledger_service';
CREATE DATABASE ledger_service OWNER ledger_service;
GRANT ALL PRIVILEGES ON DATABASE ledger_service TO ledger_service;

CREATE USER notification_service WITH PASSWORD 'notification_service';
CREATE DATABASE notification_service OWNER notification_service;
GRANT ALL PRIVILEGES ON DATABASE notification_service TO notification_service;

CREATE USER settlement_service WITH PASSWORD 'settlement_service';
CREATE DATABASE settlement_service OWNER settlement_service;
GRANT ALL PRIVILEGES ON DATABASE settlement_service TO settlement_service;

CREATE USER keycloak WITH PASSWORD 'keycloakpass';
CREATE DATABASE keycloak OWNER keycloak;
GRANT ALL PRIVILEGES ON DATABASE keycloak TO keycloak;
