package io.vertx.demo.core.mongodbclient;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.FindOptions;
import io.vertx.ext.mongo.MongoClient;


import io.vertx.demo.util.Runner;

/**
 * Created by jibou on 24/10/16.
 */
public class MongodbUpdater extends AbstractVerticle {

    // Convenience method so you can run it in your IDE
    public static void main(String[] args) {
        Runner.runClusteredExample(MongodbUpdater.class);
    }

    @Override
    public void start() throws Exception {

        // Bus settings and instance
        final String busAddress = "raw_temperature";
        final String busProcessedAdress = "median_temperature";
        final String dataRequest = "data_request";
        EventBus eb = vertx.eventBus();

        //mongoDB settings, get from config json file
        JsonObject mongoConfig = config();
        MongoClient mongoClient = MongoClient.createShared(vertx, mongoConfig);
        System.out.println("connected to mongo");

        //When new message on raw_temperature on bus
        eb.<JsonObject> consumer(busAddress, message -> {
            mongoClient.insert(busAddress, message.body(), res -> {
                System.out.println("inserted raw data "+ res.result());
            });
        });

        //When new message on median_temperature on bus
        eb.<JsonObject> consumer(busProcessedAdress, message -> {
            System.out.println("new median temp data");
            mongoClient.insert(busProcessedAdress, message.body(), res -> {
            });
        });


        //When requested for data
        eb.<JsonObject> consumer(dataRequest, message -> {
            FindOptions opts = new FindOptions();
            opts.setLimit(1);
            opts.setSort(new JsonObject().put("_id", -1));

            // get median temp
            if (message.body().getBoolean("median")) {

                mongoClient.findWithOptions(busProcessedAdress, new JsonObject(), opts, res -> {
                    if (res.succeeded()) {
                        message.reply(res.result().get(0));
                    } else {
                        message.reply(new JsonObject().put("fail", true));
                    }
                });
            }
            // get raw individual nodes temp
            else {
                opts.setFields(new JsonObject().put("id", message.body().getString("requested")));
                mongoClient.findWithOptions(busAddress, new JsonObject(), opts, res -> {
                    if (res.succeeded()) {
                        message.reply(res.result().get(0));
                    } else {
                        message.reply(new JsonObject().put("fail", true));
                    }
                });
            }
        });
        System.out.println("Ready!");
    }
}