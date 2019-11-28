# Potato Engine Cordova ONEStore Plugin

## Install

```
cordova plugin add cordova-plugin-onestore
```

You need manually replace the PUBLIC_KEY in `src/android/Hello.java` with your application's public key. (PR is welcome if you know the other way!)

## Usage

Initialization
```js
hello.init(
    () => {alert('init success')},
    e => {alert('init failed: ' + e)}
)
```

Purchase
```js
hello.purchase(
    'uid',
    'pid',
    data => {
        // let purchaseData = JSON.parse(data);
        alert(data);
    },
    error => {
        alert(error);
    }
)
```
