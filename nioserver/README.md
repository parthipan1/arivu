# Arivu NIO server :

Nio Server for high performance java application. Excellent fit for microservice . Just add annotations and bundle up with 
jar files and run it as micro service.

Sample config: =>
```java
"request":{  
      "scanpackages":[  
      ],
      "proxies":{  
         "googleSite":{  
            "header":{  
               "Expires":-1,
               "Cache-Control":"private, max-age=0",
               "Server":"clownserver",
               "X-XSS-Protection":"1; mode=block",
               "Connection":"close"
            },
            "location":"/search",
            "method":"GET",
            "proxy_pass":"https://www.google.co.in/search"
         },
         "staticContent":{  
            "header":{  
               "Expires":-1,
               "Cache-Control":"private, max-age=0",
               "Server":"clownserver",
               "X-XSS-Protection":"1; mode=block",
               "Connection":"close"
            },
            "location":"/static",
            "method":"GET",
            "dir":"/Users/parthipangounder/Downloads"
         }
      }
   },
   
```
