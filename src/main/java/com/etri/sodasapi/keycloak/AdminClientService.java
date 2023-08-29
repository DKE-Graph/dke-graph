//package com.etri.sodasapi.keycloak;
//
//import jakarta.annotation.PostConstruct;
//import org.keycloak.admin.client.Keycloak;
//import org.keycloak.representations.idm.UserRepresentation;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//
//@Service
//public class AdminClientService {
//
//    @Autowired
//    Keycloak keycloak;
//
//    private static final String REALM_NAME = "master-i";
//
//    void searchByUsername(String username, boolean exact) {
//        List<UserRepresentation> users = keycloak.realm(REALM_NAME)
//                .users()
//                .searchByUsername(username, exact);
//
//        System.out.println(users.get(0).getUsername());
//    }
//
//    void searchUsers() {
//        searchByUsername("user1", true);
//        searchByUsername("user", false);
//        searchByUsername("1", false);
//    }
//}
