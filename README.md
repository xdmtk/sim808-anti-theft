# sim808-anti-theft

---
The prototype model for an AVR/Arduino powered GPS tracking device.

### Introduction
Inspired originally by a motorcycle theft incident I experienced, the project was intended as a bare minimum hardware/software template that can be easily put together and used for remote location monitoring and alerts. A more refined, closed-source production model is currently in the works. Broken down into 4 distinct parts: 

* Hardware setup
* Embedded software
* Android monitoring app
* Web API to interface between hardware and Android app



### Hardware Requirements
[Nano V3.0 ATmega328P](https://www.aliexpress.com/item/Freeshipping-Nano-3-0-controller-compatible-for-arduino-nano-CH340-USB-driver-NO-CABLE/32241679858.html?src=google&albslr=201159365&src=google&albch=shopping&acnt=494-037-6276&isdl=y&slnk=&plac=&mtctp=&albbt=Google_7_shopping&aff_platform=google&aff_short_key=UneMJZVf&&albagn=888888&albcp=1582410664&albag=59754279756&trgt=539263010115&crea=en32241679858&netw=u&device=c&gclid=Cj0KCQjwj_XpBRCCARIsAItJiuRaJpLdSkQs1vOlb04NDmLTSdQHU52W7r2qyvTh-cdA0Qaxj77oMi8aAqX2EALw_wcB&gclsrc=aw.ds) | [3.7v to 5v Step-up PCB](https://www.banggood.com/2Pcs-5V-Lithium-Battery-Charger-Step-Up-Protection-Board-Boost-Power-Module-Micro-USB-Li-Po-Li-ion-1-p-1366320.html?gmcCountry=US&currency=USD&createTmp=1&utm_source=googleshopping&utm_medium=cpc_bgs&utm_content=frank&utm_campaign=pla-remix-usw-us-pc-0720&ad_id=368008502337&gclid=Cj0KCQjwj_XpBRCCARIsAItJiuQoIwuv7GtPCFzYof9Mpt6m7KDq2WULCAiMYCTxI_kJMSNq9UrM4AoaAhJGEALw_wcB&cur_warehouse=USA)
:---------------:|:-------------:
<img src="https://www.makerlab-electronics.com/my_uploads/2017/07/Arduino-Nano-CH340G-01-510x478.jpg" width="250"> | <img src="https://i.ebayimg.com/images/g/LXQAAOSw3YNXbpRO/s-l1600.jpg" width="250">


[LiPoly Battery 3.7v](https://www.adafruit.com/product/2011?gclid=Cj0KCQjwj_XpBRCCARIsAItJiuShiyNrZU8vEVvEWgWrAeY6Dxs5S0UdvVsq5hOFtIBx0Xe5gxjiKwQaAl4ZEALw_wcB) | [Sim808 GPRS Module](https://www.amazon.com/gp/product/B079FN45FB/ref=ppx_yo_dt_b_asin_title_o05_s00?ie=UTF8&psc=1)
:---------------:|:-------------:
<img src="http://modtronix.com/images/detailed/1/lipo1000-s603450.jpg" width="250"> | <img src="https://ae01.alicdn.com/kf/HTB13v1RadLO8KJjSZPcq6yV0FXa8.jpg" width="250">

The GPRS board specified for this project can be replaced with any board capable of serial communication and some basic AT commands (must be able to fetch coordinates and make HTTP/HTTPS requests)
### Getting Started
##### Hardware/embedded setup
To build/compile the embedded software for the Atmega328P microcontroller, you'll need to have either the Arduino IDE installed, or the Arduino CLI tools. For the bootloader, the Atmega328P requires `atmega328old`. So your compilation command would be:
```
arduino-cli compile --fqbn arduino:avr:nano:cpu=atmega328old
```

To flash the compiled executable:
```
arduino-cli upload -p /dev/{port} --fqbn arduino:avr:nano:cpu=atmega328old
```
Where the pathname is the file descriptor of your connection to the microcontroller. 

##### Android build
The code for the Android app is plug and play for Android 8.0 ( Oreo and up ), except you will need an API key from Google to access their maps API. Likewise you will need to change the endpoint locations for your own API to interface with remote data from the microcontroller. 

##### API setup
For this project I used a `t2.micro` EC2 instance from AWS, and setup a local MySQL with credentials specified in an `.env` file. The PHP written for this project can get a bit specific with directory structure, so you will likely have to modify those elements as needed.  


**NOTE**: You'll need to define the API endpoints in both the Arduino sketch, and the Android application to hit your own server. The endpoints currently referenced no longer exist. 


Design / Screenshots
---

_Inside view ( With components )_ | _Cover plate_
:---------------:|:-------------:
![](https://i.ibb.co/W0vBbX2/62575450-2224946487835713-295499775803916288-n.png)|![](https://i.ibb.co/fn24K6n/62598895-2329478867326938-7818305393280417792-n.png)

_Inside view ( Without components )_ | _Enclosed View_
:---------------:|:-------------:
![](https://i.ibb.co/XYFzrSb/64229318-354668415193931-1036831656742223872-n.png) | ![](https://i.ibb.co/nCZ6qd4/64325239-1243688255811421-8574069146017333248-n.png)






