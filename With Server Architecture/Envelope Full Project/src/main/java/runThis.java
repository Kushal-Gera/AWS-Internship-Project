import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;

public class runThis {
	
	public static void main(String[] args) throws NoSuchAlgorithmException, IOException {
		
		ImplementMethods implementation = new ImplementMethods();
		AWSCredentials credentials = new BasicAWSCredentials(UserDetails.ACCESS_KEY, UserDetails.SECRET_KEY);


		implementation.uploadFile(credentials);

		implementation.downloadFile(credentials);



	}

}
