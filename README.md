# grobid-astro

<!--[![License](http://img.shields.io/:license-apache-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0.html) -->

__Work in progress.__

The goals of this GROBID module are: 

1. to recognize in textual documents and PDF any mentions of astronomical objects,  

2. to disambiguate the mention with respect to the astronomical knowledge base SIMBAD. 

As the other GROBID models, the module relies only on machine learning and uses linear CRF. 

## Install, build, run

Building grobid-astro requires maven and JDK 1.8.  

First install the latest development version of GROBID as explained by the [documentation](http://grobid.readthedocs.org).

Copy the module quantities as sibling sub-project to grobid-core, grobid-trainer, etc.:
> cp -r grobid-astro grobid/

Try compiling everything with:
> cd PATH-TO-GROBID/grobid/

> mvn -Dmaven.test.skip=true clean install

Run some test: 
> cd PATH-TO-GROBID/grobid/grobid-astro

> mvn compile test

**The models have to be trained before running the tests!**

## Training

For training the quantity model:
> cd PATH-TO-GROBID/grobid/grobid-astro

> mvn generate-resources -Ptrain_astro

## Training data
 
... 

## Generation of training data

...


## Start the service

> mvn -Dmaven.test.skip=true jetty:run-war

Demo/console web app is then accessible at ```http://localhost:8080```

Using ```curl``` POST/GET requests:


```
curl -X POST -d "text=Look at GRB 020819, on the right." localhost:8080/processAstroText
```

```
curl -GET --data-urlencode "text=Look at Andromeda bellow the North Star." localhost:8080/processAstroText
```

## License

...