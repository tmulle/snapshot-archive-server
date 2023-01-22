package org.acme;

import io.quarkus.runtime.annotations.RegisterForReflection;
import org.acme.service.MongoGridFSService;

/**
 * This is used to register 3rd part libs when building a native image
 * of this project
 *
 * We need the FileInfo from the 3rd party lib
 *
 * Found: https://quarkus.io/guides/writing-native-applications-tips
 *
 * @author tmulle
 */
@RegisterForReflection(targets = {MongoGridFSService.FileInfo.class})
public class MyReflectionConfiguration {

}
