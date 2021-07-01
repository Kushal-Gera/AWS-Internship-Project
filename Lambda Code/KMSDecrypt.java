import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.AWSKMSClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.AmazonS3EncryptionClientBuilder;
import com.amazonaws.services.s3.event.S3EventNotification;
import com.amazonaws.services.s3.model.CryptoConfiguration;
import com.amazonaws.services.s3.model.EncryptionMaterials;
import com.amazonaws.services.s3.model.KMSEncryptionMaterialsProvider;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.StaticEncryptionMaterialsProvider;


public class LambdaTrigger implements RequestHandler<S3EventNotification, String> {

	
	final String ACCESS_KEY = "**********";
	final String SECRET_KEY = "***********";
	
	final String ACCESS_KEY_B = "***********";
	final String SECRET_KEY_B = "************";
	String CMK_KEY_ARN = "***************";
	String CLIENT_REGION = "ap-south-1";
	String BUCKET_NAME = "files-encrypted";
	
		public String handleRequest(S3EventNotification s3Event, Context context) {
		
		//Get the File details which triggered the Lambda Function
		S3EventNotification.S3EventNotificationRecord record = s3Event.getRecords().get(0);
        
        String keyName = record.getS3().getObject().getKey().replace('+', ' ');
        
			try {
				keyName = URLDecoder.decode(keyName, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			 AWSCredentials credentials = new BasicAWSCredentials(ACCESS_KEY, SECRET_KEY);
			 AWSCredentials credentials_b = new BasicAWSCredentials(ACCESS_KEY_B, SECRET_KEY_B);
		       
					 AmazonS3 s3Client = AmazonS3ClientBuilder.standard().
							 withCredentials(new AWSStaticCredentialsProvider(credentials_b)).withRegion(CLIENT_REGION).build();
					 
					KMSEncryptionMaterialsProvider materialProvider = new KMSEncryptionMaterialsProvider(CMK_KEY_ARN);
					CryptoConfiguration cryptoConfig = new CryptoConfiguration()
			                .withAwsKmsRegion(RegionUtils.getRegion(CLIENT_REGION));
					
			        AmazonS3 s3EncryptionClient = AmazonS3EncryptionClientBuilder.standard()
			                .withCredentials(new AWSStaticCredentialsProvider(credentials))
			                .withEncryptionMaterials(materialProvider)
			                .withCryptoConfiguration(cryptoConfig)
			                .withRegion(CLIENT_REGION).build();
			        
					S3Object downloadedObject = s3EncryptionClient.getObject(BUCKET_NAME, keyName);
					S3ObjectInputStream inputStream = downloadedObject.getObjectContent();
					 
					s3Client.putObject("open-download-bucket", keyName, inputStream, new ObjectMetadata());
					return null;					 
	}
}
