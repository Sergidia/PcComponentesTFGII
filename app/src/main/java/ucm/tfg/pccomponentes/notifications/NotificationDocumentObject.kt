package ucm.tfg.pccomponentes.notifications

class NotificationDocumentObject (notifPush: Boolean, notifEmail: Boolean, tokenID: String) {

    private var push: Boolean = notifPush
    private var email: Boolean = notifEmail
    private var token: String = tokenID

    constructor() : this(false, false, ""){}

    fun getPush(): Boolean {
        return push
    }

    fun setPush(notifPush: Boolean) {
        push = notifPush;
    }

    fun getEmail(): Boolean {
        return email
    }

    fun setEmail(notifEmail: Boolean) {
        email = notifEmail;
    }

    fun getToken(): String {
        return token
    }

    fun setToken(tokenID: String) {
        token = tokenID;
    }

}