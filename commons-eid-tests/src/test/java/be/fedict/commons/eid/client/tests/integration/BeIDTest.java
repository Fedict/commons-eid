package be.fedict.commons.eid.client.tests.integration;

import be.fedict.commons.eid.client.BeIDCard;
import be.fedict.commons.eid.client.BeIDCards;
import be.fedict.commons.eid.client.CancelledException;
import be.fedict.commons.eid.client.FileType;
import be.fedict.commons.eid.client.event.BeIDCardListener;

import static org.junit.Assert.assertNotNull;

public abstract class BeIDTest {

	protected BeIDCard getBeIDCard() throws CancelledException {
		BeIDCards beIDCards = new BeIDCards(new TestLogger());
		BeIDCard beIDCard = beIDCards.getOneBeIDCard();
		assertNotNull(beIDCard);

		beIDCard.addCardListener(new DummyBeIDCardListener());

		return beIDCard;
	}

	private static class DummyBeIDCardListener implements BeIDCardListener {
		@Override
		public void notifyReadProgress(FileType fileType, int offset, int estimatedMaxSize) {
			System.out.println("read progress of " + fileType.name() + ":" + offset + " of " + estimatedMaxSize);
		}

		@Override
		public void notifySigningBegin(FileType keyType) {
			System.out.println("signing with " + (keyType == FileType.AuthentificationCertificate ? "authentication" : "non-repudiation") + " key has begun");
		}

		@Override
		public void notifySigningEnd(FileType keyType) {
			System.out.println("signing with " + (keyType == FileType.AuthentificationCertificate ? "authentication" : "non-repudiation") + " key has ended");
		}
	}
}
