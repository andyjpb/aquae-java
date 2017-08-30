
import uk.gov.Aquae.*;


public class testclient {

	static {
		// Require assertions to be enabled.
		// We need to make sure that we never use assert in a way that
		// can enable a denial of service attack.
		boolean assertsEnabled = false;
		assert assertsEnabled = true; // Intentional side effect!!!
		if (!assertsEnabled)
			throw new RuntimeException("Asserts must be enabled!!!");
	}

	public static void main(String[] args) throws Exception {

		System.out.println("Test client...");

		AquaeMetadata md = new AquaeMetadata("../Metadata.pb.bin");

		// try to run a query
		// find the query in the metadata
		AquaeQuery q = md.findQuery("bb?");

		System.out.println("q is " + q);

	}
}

