package com.example.demo.servicesMysql;

import java.util.List;
import java.util.Optional;
import com.example.demo.entitiesMysql.UserEntity;

public interface UserService {
    UserEntity saveUser(UserEntity userEntity);
    List<UserEntity> getAllUsers();
    Optional<UserEntity> getUserById(Long id);
    Optional<UserEntity> getUserByEmail(String email);
    void deleteUser(Long id);
    UserEntity updateUser(Long id, UserEntity userDetails);
}