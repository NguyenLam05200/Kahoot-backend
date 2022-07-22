# Manual

Mono repo contains services for Kahoot. 
1. Auth Service (auth/)
2. CRUD APIs (user-profile)

## Deployment

1. Dockerize

- auth-service
```bash
 $ docker build -t vng4dinnn/auth-service:<verion> . 
 ```

 user profile
```bash
 $ docker build -t vng4dinnn/user-profile:<verion> . 
```

2. Start services

- auth-service
```bash
 $ docker run -p 8080:8080  vng4dinnn/auth-service:<version> 
 ```

user profile
```bash
 $ docker run -p 9090:9090  vng4dinnn/user-profile:<version>  
```

3. Remotely Run
ssh to remote server, and execute commands in #2 Start services

## Dependencies
1. MongoDb. Install local