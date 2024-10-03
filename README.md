# Potato Engine Cordova ONEStore Plugin

## Install

```
cordova plugin add cordova-plugin-onestore
```

## Usage

Initialization
```js
onestore_plugin.init(
    () => {alert('init success')},
    e => {alert('init failed: ' + e)}
)
```

Purchase
```js
onestore_plugin.purchase(
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
