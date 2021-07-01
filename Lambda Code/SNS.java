import java.net.URLDecoder;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.AmazonS3EncryptionClientBuilder;
import com.amazonaws.services.s3.event.S3EventNotification;
import com.amazonaws.services.s3.event.S3EventNotification.S3EventNotificationRecord;
import com.amazonaws.services.s3.model.CryptoConfiguration;
import com.amazonaws.services.s3.model.KMSEncryptionMaterialsProvider;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;


public class LambdaTrigger implements RequestHandler<S3EventNotification, String> {

	String ACCESS_KEY = "";
	String SECRET_KEY = "";
	String kms_cmk_keyID = "";
	String CLIENT_REGION = "ap-south-1";
	String DEST_BUCKET_NAME = "files-encrypted";

	@Override
	public String handleRequest(S3EventNotification s3Event, Context context) {

        try {
//			**************************** SNS EMAIL CODE ********************************
			AmazonSNS snsClient = AmazonSNSClient.builder()
			          .withCredentials(new AWSStaticCredentialsProvider(credentials))
			          .withRegion(CLIENT_REGION).build();
			
			CreateTopicRequest createTopicRequest = new CreateTopicRequest(DEST_BUCKET_NAME + "_notify");
			CreateTopicResult createTopicResponse = snsClient.createTopic(createTopicRequest);
			String TOPIC_ARN = createTopicResponse.getTopicArn();
			
			snsClient.publish(
					TOPIC_ARN, 
					keyName + " has been uploaded !", 
					"Sub: New File Uploaded to AWS S3 Bucket");
			System.out.println("Email notification Send");
	
			
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(e.getMessage());
		}

		return null;
	}

	
	
}
