
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


		// load our private key
		// know who we are
		// load metadata

		AquaeMetadata md = new AquaeMetadata("../Metadata.pb.bin");


		// try to run a query
		// find the query in the metadata
		AquaeQuery q = md.findQuery("bb?");

		System.out.println("q is " + q);

		// plan it
		// Determine the choices and choose one
		//AquaeQuery.Choice[] c = q.getChoices();
		//AquaeQuery.Plan p = q.plan(c[0]);

		// list p.identity_requirements

		AquaeDataStructures.Person subject  = new AquaeDataStructures.Person("Bennett", "SW11 4BU", 1983, 5, "2017/08/16");
		AquaeDataStructures.Person delegate = subject;
		AquaeDataStructures.Agent  agent    = null;

		// p.set_identity(subject);

		// p.get_consent_url();
		// ...
		// p.consent(); // we need a better name for this. it's asking the consent server to sign the plan.

		// run it

		//AquaeQuery.Result r = p.execute();



	}
}

