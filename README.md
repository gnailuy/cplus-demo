## CPlus Service Demo

```
openssl genrsa | openssl pkcs8 -topk8 -nocrypt -out rsa_private_key.pem
openssl rsa -in rsa_private_key.pem -pubout -out rsa_public_key.pem
# Exchange RSA Key with UMeng
mvn clean package
java -cp target/cplus-demo-0.0.1-uber.jar com.umeng.cplus.DemoClient rsa_private_key.pem
```

