package com.adonai.wallet.entities;

import com.adonai.wallet.DatabaseDAO;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface EntityDescriptor {

    DatabaseDAO.EntityType type();

}
