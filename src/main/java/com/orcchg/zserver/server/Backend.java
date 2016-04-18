package com.orcchg.zserver.server;

import com.google.gson.Gson;
import com.orcchg.zserver.utility.Utility;
import rx.Observable;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.List;
import java.util.Map;

class Backend {

    static class NoSuchMethodException extends Exception {
        NoSuchMethodException(String message) {
            super(message);
        }
    };

    static Observable<String> invokeMethod(DataProvider dataProvider, URL url)
            throws UnsupportedEncodingException, NoSuchMethodException {
        String path = url.getPath();
        Gson gson = new Gson();
        Map<String, List<String>> params;

        params = Utility.splitQuery(url);
        switch (path) {
            case "/customers/":
                int limit = Integer.parseInt(params.get("limit").get(0));
                int offset = Integer.parseInt(params.get("offset").get(0));
                return dataProvider.getCustomers(limit, offset).map(gson::toJson);
            case "/address/":
                int addressId = Integer.parseInt(params.get("address_id").get(0));
                return dataProvider.getAddress(addressId).map(gson::toJson);
        }

        throw new NoSuchMethodException(path);
    }
}
