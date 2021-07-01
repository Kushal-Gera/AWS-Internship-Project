package com.test;

import java.io.IOException;
import java.net.URLDecoder;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.AWSKMSClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.AmazonS3EncryptionClientBuilder;
import com.amazonaws.services.s3.event.S3EventNotification;
import com.amazonaws.services.s3.model.EncryptionMaterials;
import com.amazonaws.services.s3.model.KMSEncryptionMaterialsProvider;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.StaticEncryptionMaterialsProvider;





public class App implements RequestHandler<S3EventNotification, String> {

	
	Logger log = LoggerFactory.getLogger(App.class);
	final String ACCESS_KEY = "********";
	final String SECRET_KEY = "********";
	String CMK_KEY_ARN = "**************";
	String CLIENT_REGION = "ap-south-1";
	String BUCKET_NAME = "new-bucket-download";  // Destination bucket for encrypted file
	


	public String handleRequest(S3EventNotification s3Event, Context context) {
		
		log.info("Lambda function is invoked:" + s3Event.toJson());
		
		
		//Get the File details which triggered the Lambda Function
		S3EventNotification.S3EventNotificationRecord record = s3Event.getRecords().get(0);
       		String bucketName = record.getS3().getBucket().getName();
        	String keyName = record.getS3().getObject().getKey().replace('+', ' ');
		
		//Encrypting below
        	try {
			keyName = URLDecoder.decode(keyName, "UTF-8");
			
			AWSCredentials credentials = new BasicAWSCredentials(ACCESS_KEY, SECRET_KEY);
		        
		        KeyGenerator symKeyGenerator;
		        SecretKey symKey_file;
			
			symKeyGenerator = KeyGenerator.getInstance("AES");
			symKeyGenerator.init(256);
			symKey_file = symKeyGenerator.generateKey();
					
			String encodedKey = Base64.getEncoder().encodeToString(symKey_file.getEncoded());
					
			EncryptionMaterials encryptionMaterials = new EncryptionMaterials(symKey_file);
			AmazonS3 s3EncryptionClient = AmazonS3EncryptionClientBuilder.standard()
					            .withCredentials(new AWSStaticCredentialsProvider(credentials))
					            .withEncryptionMaterials(new StaticEncryptionMaterialsProvider(encryptionMaterials))
					            .withRegion(CLIENT_REGION)
					            .build();
					 
			AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
				 	     .withCredentials(new AWSStaticCredentialsProvider(credentials))
				 	     .withRegion(CLIENT_REGION).build();
					 
			S3Object downloadedObject = s3Client.getObject(bucketName, keyName); 
			ObjectMetadata metadata = s3Client.getObjectMetadata(bucketName, keyName);
			S3ObjectInputStream inputStream = downloadedObject.getObjectContent();
					 
					 
					 
			s3EncryptionClient.putObject(new PutObjectRequest(
					            BUCKET_NAME,
					            keyName,
					           inputStream,
					            metadata));
					 
	    		log.info("File encrypted and uploaded into the S3 bucket");
	     			 					 
			AWSKMS kmsClient = AWSKMSClientBuilder.standard()
								.withRegion(CLIENT_REGION)
								.withCredentials(new AWSStaticCredentialsProvider(credentials))
								.build();
			
			String s3ObjectKeyName = keyName + "Key";
			String s3ObjectContent = encodedKey;
			
			AmazonS3 s3EncryptionClient_key = AmazonS3EncryptionClientBuilder.standard()
			    				  .withCredentials(new AWSStaticCredentialsProvider(credentials))
							  .withEncryptionMaterials(new KMSEncryptionMaterialsProvider(CMK_KEY_ARN))
							  .withRegion(CLIENT_REGION)
							  .withKmsClient(kmsClient)
							  .build();


			s3EncryptionClient_key.putObject(BUCKET_NAME, s3ObjectKeyName, s3ObjectContent);

			log.info("Key encrypted and uploaded into S3 bucket");
		
			s3Client.deleteObject(bucketName, keyName);
	     			
	     		log.info("Unencrypted File deleted");
			
       		} catch (NoSuchAlgorithmException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}
}
