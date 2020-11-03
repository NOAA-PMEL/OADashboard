/**
 * 
 */
package gov.noaa.pmel.dashboard.client;

public enum MessageInformationType {
    PLAIN("images/blank_1px.gif"),
    QUESTION("images/questionMark_64px.png"),
    WARNING("images/warning_64px.png"),
    CRITICAL("images/warning_64px-redblack.png");
    
    private String _iconSrc;
    
    private MessageInformationType(String iconSrc) {
        this._iconSrc = iconSrc;
    }
    String iconSrc() { return _iconSrc; }
}