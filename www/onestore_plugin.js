/*global cordova, module*/

module.exports = {
    init: function(publicKey, successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "OneStorePlugin", "init", [publicKey]);
    },
    purchase: function(payload, productId, successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "OneStorePlugin", "purchase", [payload, productId]);
    },
    consume: function(purchaseData, successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "OneStorePlugin", "consume", [purchaseData]);
    },

    getPurchases: function(successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "OneStorePlugin", "get_purchases", []);
    },

    getProducts: function(productsIds, successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "OneStorePlugin", "get_products", [productsIds]);
    }
    
};
