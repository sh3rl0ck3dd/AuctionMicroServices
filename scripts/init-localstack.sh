#!/bin/bash
set -e

echo "Creating auction-end-queue in LocalStack SQS..."
aws --endpoint-url=http://localhost:4566 sqs create-queue --queue-name auction-end-queue --region us-east-1

echo "Queue URL:"
aws --endpoint-url=http://localhost:4566 sqs get-queue-url --queue-name auction-end-queue --region us-east-1

echo "Done."
