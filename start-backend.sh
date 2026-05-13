#!/bin/bash
service mysql start
service redis-server start 2>/dev/null
cd /mnt/d/Code/restaurant/restaurant-backend
exec mvn spring-boot:run
