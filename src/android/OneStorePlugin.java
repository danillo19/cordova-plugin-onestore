package com.example.plugin;

import android.app.Activity;
import android.util.Log;

import androidx.annotation.NonNull;

import com.gaa.sdk.auth.GaaSignInClient;
import com.gaa.sdk.auth.OnAuthListener;
import com.gaa.sdk.auth.SignInResult;
import com.gaa.sdk.iap.ConsumeListener;
import com.gaa.sdk.iap.ConsumeParams;
import com.gaa.sdk.iap.IapResult;
import com.gaa.sdk.iap.IapResultListener;
import com.gaa.sdk.iap.ProductDetail;
import com.gaa.sdk.iap.ProductDetailsListener;
import com.gaa.sdk.iap.ProductDetailsParams;
import com.gaa.sdk.iap.PurchaseClient;
import com.gaa.sdk.iap.PurchaseClient.ProductType;
import com.gaa.sdk.iap.PurchaseClientStateListener;
import com.gaa.sdk.iap.PurchaseData;
import com.gaa.sdk.iap.PurchaseFlowParams;
import com.gaa.sdk.iap.PurchasesUpdatedListener;
import com.gaa.sdk.iap.QueryPurchasesListener;
import com.gaa.sdk.iap.Security;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

enum PluginAction {
    INIT,
    PURCHASE,
    CONSUME,
    GET_PURCHASES,
    GET_PRODUCTS,
}

public class OneStorePlugin extends CordovaPlugin {
    PurchaseClient mPurchaseClient = null;
    ConsumeListener mConsumeListener = null;
    String TAG = "ONESTORE_TEST";

    String publicKey = "";

    Activity mAct;

    private CallbackContext purchaseContext;
    private CallbackContext consumeContext;
    private CallbackContext initContext;

    private CallbackContext loadPurchasesContext;

    private CallbackContext loadProductsContext;

    public void getAct() {
        cordova.setActivityResultCallback(this); // Required
        mAct = this.cordova.getActivity();
    }

    public void init(String pk, CallbackContext callbackContext) {
        initContext = callbackContext;

        publicKey = pk;

        PurchaseClientStateListener purchaseClientStateListener = new PurchaseClientStateListener() {
            @Override
            public void onSetupFinished(IapResult iapResult) {
                if (iapResult.isSuccess()) {
                    //loadPurchases();
                    if (initContext != null) {
                        initContext.success(0);
                        initContext = null;
                    }
                } else if (iapResult.getResponseCode() == PurchaseClient.ResponseCode.RESULT_NEED_UPDATE) {
                    mPurchaseClient.launchUpdateOrInstallFlow(mAct, null);
                } else if (iapResult.getResponseCode() == PurchaseClient.ResponseCode.RESULT_NEED_LOGIN) {
                    if (initContext != null) {
                        initContext.success(iapResult.getResponseCode());
                        initContext = null;
                    }

                    getAct();
                    GaaSignInClient signInClient = GaaSignInClient.getClient(mAct);
                    signInClient.launchSignInFlow(mAct, new OnAuthListener() {
                        @Override
                        public void onResponse(@NonNull SignInResult signInResult) {
                            int code = signInResult.getCode();
                            String message = signInResult.getMessage();
                            Log.i(TAG, "Code: " + code + "; Message: " + message);
                        }
                    });

                } else {
                    Log.e(TAG, "Code: " + iapResult.getResponseCode() + "; msg: " + iapResult.getMessage());
                    if (initContext != null) {
                        initContext.error(iapResult.getMessage());
                        initContext = null;
                    }
                }
            }

            @Override
            public void onServiceDisconnected() {
                Log.e(TAG, "ONE store service disconnected!");
            }
        };

        IapResultListener updateOrInstallFlowListener = new IapResultListener() {
            @Override
            public void onResponse(IapResult iapResult) {
                if (iapResult.isSuccess()) {
                    Log.i(TAG, "launchUpdateOrInstallFlow was successful");
                    mPurchaseClient.startConnection(purchaseClientStateListener);
                } else {
                    Log.i(TAG, "launchUpdateOrInstallFlow failed: " + iapResult.getMessage() + " ;code: " + iapResult.getResponseCode());
                }
            }
        };


        PurchasesUpdatedListener purchasesUpdatedListener = new PurchasesUpdatedListener() {
            @Override
            public void onPurchasesUpdated(IapResult iapResult, List<PurchaseData> purchases) {
                if (iapResult.isSuccess() && purchases != null) {
                    for (PurchaseData purchase : purchases) {
                        Log.d(TAG, "successful onPurchasesUpdated; product: " + purchase.getProductId());
                        //consumeItem(purchase);

                        if (purchaseContext != null) {
                            boolean isValidPurchase = Security.verifyPurchase(publicKey, purchase.getOriginalJson(), purchase.getSignature());

                            if (!isValidPurchase) {
                                purchaseContext.error("invalid purchase");
                                purchaseContext = null;
                                return;
                            }

                            purchaseContext.success(purchase.getOriginalJson());
                            purchaseContext = null;
                        }
                    }
                } else if (iapResult.getResponseCode() == PurchaseClient.ResponseCode.RESULT_NEED_UPDATE) {
                    mPurchaseClient.launchUpdateOrInstallFlow(mAct, updateOrInstallFlowListener);
                } else if (iapResult.getResponseCode() == PurchaseClient.ResponseCode.RESULT_NEED_LOGIN) {
                    mPurchaseClient.launchLoginFlowAsync(mAct,
                            iap -> Log.d(TAG, "onResponse of IapResultListener: " + iap.getMessage() + " ;code: " + iap.getResponseCode()));
                } else {
                    Log.e(TAG, "one store onPurchasesUpdate response msg: " + iapResult.getMessage() + "; code: " + iapResult.getResponseCode());
                }


                if (purchaseContext != null) {
                    purchaseContext.error("error:" + iapResult.getMessage());
                    purchaseContext = null;
                }
            }
        };


        if (mPurchaseClient == null) {
            getAct();

            mPurchaseClient = PurchaseClient.newBuilder(mAct).setBase64PublicKey(publicKey).setListener(purchasesUpdatedListener).build();
        }


        mConsumeListener = (iapResult, purchaseData) -> {
            if (iapResult.isFailure()) {
                if (consumeContext != null) {
                    consumeContext.error(iapResult.getMessage());
                    consumeContext = null;
                }
                return;
            }

            Log.d(TAG, "TODO consumeAsync onSuccess, " + purchaseData.toString());
            if (consumeContext != null) {
                String test = purchaseData.getOriginalJson();
                consumeContext.success(test);
                consumeContext = null;
            }

        };

        mPurchaseClient.startConnection(purchaseClientStateListener);
    }

    public void purchase(String purchasePayload, String productId, CallbackContext callbackContext) {
        purchaseContext = callbackContext;

        cordova.getActivity().runOnUiThread(() -> {
            PurchaseFlowParams purchaseFlowParams = PurchaseFlowParams.newBuilder()
                    .setProductType(ProductType.INAPP)
                    .setProductId(productId)
                    .setDeveloperPayload(purchasePayload)
                    .setQuantity(1)
                    .build();

            mPurchaseClient.launchPurchaseFlow(cordova.getActivity(), purchaseFlowParams);
        });
    }

    public void consume(String rawPurchasesData, CallbackContext callbackContext) {
        consumeContext = callbackContext;

        PurchaseData purchaseData = new PurchaseData(rawPurchasesData);

        consumeItem(purchaseData);
    }

    @Override
    public boolean execute(String actionStr, JSONArray args, CallbackContext callbackContext) throws JSONException {
        try {
            PluginAction action = PluginAction.valueOf(actionStr.toUpperCase(Locale.ENGLISH));

            switch (action) {
                case INIT: {
                    String publicKey = args.getString(0);
                    init(publicKey, callbackContext);
                    return true;
                }
                case PURCHASE: {
                    String purchasePayload = args.getString(0);
                    String productId = args.getString(1);

                    purchase(purchasePayload, productId, callbackContext);
                    return true;
                }
                case CONSUME: {
                    String purchaseData = args.getString(0);
                    consume(purchaseData, callbackContext);
                    return true;
                }
                case GET_PURCHASES:
                    loadPurchases(callbackContext);
                    return true;
                case GET_PRODUCTS:
                    JSONArray productsIdsJson = args.getJSONArray(0);
                    List<String> productIds = new ArrayList<>(productsIdsJson.length());
                    
                    for (int i = 0;i < productsIdsJson.length();i++) {
                        productIds.add(productsIdsJson.getString(i));
                    }
                    
                    loadProducts(callbackContext, productIds);
            }
        } catch (IllegalArgumentException ex) {
            callbackContext.error("unsupported action: " + actionStr);
            return false;
        }

        return false;
    }

    private void loadProducts(CallbackContext callbackContext, List<String> productsIds) {
        loadProductsContext = callbackContext;

        ProductDetailsParams params = ProductDetailsParams.newBuilder()
                .setProductIdList(productsIds)
                .setProductType(ProductType.INAPP)
                .build();

        mPurchaseClient.queryProductDetailsAsync(params, (iapResult, productsList) -> {
            JsonArray productsSerialized = new JsonArray();

            for (ProductDetail productDetail : productsList) {
                JsonObject parsed = JsonParser.parseString(productDetail.getOriginalJson()).getAsJsonObject();
                productsSerialized.add(parsed);
            }

            if (loadProductsContext != null) {
                String output = productsSerialized.toString();
                loadProductsContext.success(output);
                loadProductsContext = null;
            }
        });
    }

    private void loadPurchases(CallbackContext callbackContext) {
        loadPurchasesContext = callbackContext;

        Log.d(TAG, "loadPurchases()");
        loadPurchase(ProductType.INAPP);
    }

    private void onLoadPurchaseInApp(List<PurchaseData> purchaseDataList) {
        Log.i(TAG, "onLoadPurchaseInApp() :: purchaseDataList - " + purchaseDataList.toString());

        JsonArray purchasesToSync = new JsonArray();

        for (PurchaseData purchaseData : purchaseDataList) {
            if (purchaseData.getPurchaseState() == PurchaseData.PurchaseState.PURCHASED) {
                JsonObject parsed = JsonParser.parseString(purchaseData.getOriginalJson()).getAsJsonObject();
                purchasesToSync.add(parsed);
            }
        }

        if (loadPurchasesContext != null) {
            String output = purchasesToSync.toString();
            loadPurchasesContext.success(output);
            loadPurchasesContext = null;
        }
    }

    private void consumeItem(final PurchaseData purchaseData) {
        ConsumeParams consumeParams = ConsumeParams.newBuilder()
                .setPurchaseData(purchaseData)
                .build();


        Log.e(TAG, "consumeItem() :: productId - " + purchaseData.getProductId() + " orderId -" + purchaseData.getOrderId());

        if (mPurchaseClient == null) {
            Log.d(TAG, "PurchaseClient is not initialized");
            return;
        }

        mPurchaseClient.consumeAsync(consumeParams, mConsumeListener);
    }

    QueryPurchasesListener mQueryPurchaseListener = (iapResult, list) -> {
        if (iapResult.isFailure()) {
            Log.d(TAG, "onPurchasesResponse failed: " + iapResult.getMessage());
            return;
        }

        if (list == null) {
            Log.d(TAG, "empty purchasesData list in onPurchasesResponse");
            return;
        }

        Log.d(TAG, "queryPurchasesAsync onSuccess, " + list);
        onLoadPurchaseInApp(list);
    };

    private void loadPurchase(String productType) {
        Log.i(TAG, "loadPurchase() :: productType - " + productType);

        mPurchaseClient.queryPurchasesAsync(productType, mQueryPurchaseListener);
    }

}
