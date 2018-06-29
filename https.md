A: generate private key and public key and pass public key to B
B: receive public key of A, and encrypt password with public key of A
A: recieve encrypted password and decrypt with private key of A

A: transport certificate which include information of A, A public key and digital signature(infomation + public key => hash string => encrypt by CA private key) to B
B: decrypt digital signature with CA public key and compare with hash string
B: encrypt password with A public key and transport to A
A: recieve encrypted password and decrypt with A private key

Client: request for certificate
Charles: Intercept client request and send request to server
Server: response with certificate
Charles: Intercept server response and send charles certifate to client
Client: trust charles certificate, encrypt password with charles public key and send to server
Charles: Intercept client request, decrypt with charles private key, encrypt with server public key and send to server
