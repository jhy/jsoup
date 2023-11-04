This directory contains resources for a self-signed TLS certificate, used in jsoup's local integration tests.

Create the certificate:

```sh
openssl genrsa 2048 > server.key
chmod 400 server.key
openssl req -new -x509 -config cert.conf -nodes -sha256 -days 36135 -key server.key -out server.crt
```

Create the Java key store. Used by server, and trusted by client, in `TestServer.java`:
```sh
openssl pkcs12 -export -in server.crt -inkey server.key -out server.p12 -name jsoup -passout pass:hunter2
keytool -importkeystore -srckeystore server.p12 -srcstoretype PKCS12 -destkeystore server.pfx -deststoretype PKCS12  -srcstorepass hunter2 -deststorepass hunter2
```
