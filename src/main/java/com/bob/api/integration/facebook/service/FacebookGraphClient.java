package com.bob.api.integration.facebook.service;

import java.util.List;

public interface FacebookGraphClient {

    String exchangeLongLivedToken(String shortLivedToken);

    List<FacebookAccountPage> listPages(String userAccessToken);

    FacebookPageDetails fetchPageDetails(String pageAccessToken);

    void subscribePage(String pageId, String pageAccessToken);

    FacebookSendResult sendText(String pageAccessToken, String recipientPsid, String text);

    FacebookUserProfile fetchUserProfile(String pageAccessToken, String psid);

    record FacebookAccountPage(String id, String name, String accessToken) {
    }

    record FacebookPageDetails(String providerName, String instagramId) {
    }

    record FacebookSendResult(String messageId, String errorCode, String errorMessage) {
        public boolean failed() {
            return errorMessage != null && !errorMessage.isBlank();
        }

        public boolean authorizationError() {
            if (errorMessage == null) {
                return false;
            }
            return errorMessage.contains("The session has been invalidated")
                    || errorMessage.contains("Error validating access token");
        }
    }

    record FacebookUserProfile(String firstName, String lastName) {
        public String displayName() {
            String first = firstName == null || firstName.isBlank() ? "John" : firstName;
            String last = lastName == null || lastName.isBlank() ? "Doe" : lastName;
            return first + " " + last;
        }
    }
}
