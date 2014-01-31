package uk.ac.ed.insectlab.ant;


public interface NetworkControl {
	public enum NetworkMessage {
		NEW_LOOK_AROUND("new_look_around");
		
		private String text;

		NetworkMessage(String string) {
			text = string;
		}
		
		public String getText() {
			return text;
		}
	}
	public void sendMessage(NetworkMessage message);
	public void sendPicture(RoboPicture roboPicture);
}