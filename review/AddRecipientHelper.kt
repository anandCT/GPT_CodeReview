package com.firstrepublic.zelle.common.utils

import com.firstrepublic.zelle.data.dto.response.ZelleContactType
import com.firstrepublic.zelle.data.dto.response.contacts.Contact
import com.firstrepublic.zelle.domain.dto.*
import com.firstrepublic.zelle.presentation.addrecipient.placeholder.*

class AddRecipientHelper {

    /**
     * [SelectedRecipientType] for the recipient selected on the AddRecipientFragment
     */
    private var selectedRecipient: SelectedAddRecipient? = null

    /**
     * Add details screen object to use for duplicate flow.
     */
    var addedRecipient: SelectedAddRecipient? = null

    //List of contacts which are duplicates
    private var duplicateContact: Contact? = null

    /**
     * Creating temp object for recipient added by user in add details screen first time.
     */
    var tempRecipient: SelectedAddRecipient? = null

    /**
     *To prevent data to modify on update details screen if duplication found.
     */
    var addDetailsBankUiData: MutableList<AddBankAccUiData> = mutableListOf()

    var firstName: String? = null
    var lastName: String? = null
    var nickName: String? = null
    var businessName: String? = null
    var firstNameValid = false
    var nickNameValid = false
    var lastNameValid = false
    var businessNameValid = false
    var isFromEdit: Boolean? = null
    var isFromRecipientEdit: Boolean = false
    var isTokenDuplication: Boolean? = null

    //While checking for the duplicates, set this type to identify what kind of duplication it is
    var duplicateRecipientType: DuplicateRecipientType? = null

    /**
     * List of all mobile no added on the UI
     */
    private var cachedMobileNoUiList = mutableListOf<AddMobileNoUiData>()

    /**
     * List of all mobile no added on the UI
     */
    private var cachedEmailUiList = mutableListOf<AddEmailUiData>()

    /**
     * List of all bank acc added on the UI
     */
    private var cachedBankAccUiList = mutableListOf<AddBankAccUiData>()

    /**
     * To identify the add recipient flow.
     * Should be any one of [AddRecipientMethod.ADD_MANUALLY] or [AddRecipientMethod.IMPORT_FROM_DEVICE]
     */
    private lateinit var addRecipientMethod: AddRecipientMethod

    /**
     * check for same name duplication in business or individual
     */
    fun checkForNameDuplication(
        recipientType: SelectedRecipientType? = null,
        cachedRecipientList: List<Contact>
    ): DuplicateRecipientType {
        var selectedRecipientType = recipientType
        if (selectedRecipientType == null)
            selectedRecipientType = addedRecipient?.recipientType
        //Check for duplication
        return when (selectedRecipientType) {
            SelectedRecipientType.INDIVIDUAL -> {
                checkForIndividualDuplication(cachedRecipientList)
            }
            SelectedRecipientType.BUSINESS -> {
                checkForBusinessDuplication(cachedRecipientList)
            }
            null -> DuplicateRecipientType.NO_DUPLICATION
        }
    }

    /**
     * Iterate through cached recipient list (Captured from getContactTokens API) and check for duplication
     *
     * @param contact list of recipients cached from getContactTokens API response.
     * @return [DuplicateRecipientType] type of duplicate.
     */
    private fun checkForIndividualDuplication(
        contact: List<Contact>
    ): DuplicateRecipientType {
        for (item in contact) {
            if (isNameDuplication(item)) {
                if (item.nickName.isNullOrEmpty() && nickName.isNullOrEmpty()) {
                    duplicateContact = item
                    return DuplicateRecipientType.NAME
                } else if (isNickNameDuplication(item)) {
                    duplicateContact = item
                    return DuplicateRecipientType.NAME_WITH_NICKNAME
                }
            }
        }
        return DuplicateRecipientType.NO_DUPLICATION
    }

    /**
     * Iterate through cached recipient list (Captured from getContactTokens API) and check for duplication
     *
     * @param contact list of recipients cached from getContactTokens API response.
     * @return [DuplicateRecipientType] type of duplicate.
     */
    private fun checkForBusinessDuplication(contact: List<Contact>): DuplicateRecipientType {
        //check for duplication from cached list for business
        for (item in contact) {
            if (isBusinessDuplication(item)) {
                duplicateContact = item
                return DuplicateRecipientType.BUSINESS_NAME
            }
        }
        return DuplicateRecipientType.NO_DUPLICATION
    }


    fun isBusinessDuplication(contact: Contact?): Boolean {
        if (isFromRecipientEdit && compareBusinessWithSelected(null, contact)) {
            return false
        }
        return (!contact?.businessName.isNullOrEmpty()) && contact?.businessName?.trimLowerCase() == businessName?.trimLowerCase()
    }

    fun compareBusinessWithSelected(recipient: SelectedAddRecipient?, contact: Contact?): Boolean {
        val businessName =
            if (recipient != null)
                recipient.businessName?.trimLowerCase()
            else
                addedRecipient?.businessName?.trimLowerCase()
        return contact?.businessName?.trimLowerCase() == businessName?.trimLowerCase()
    }

    fun isNameDuplication(contact: Contact?): Boolean {
        if (isFromRecipientEdit && compareNameWithSelected(null, contact)) {
            return false
        }
        return contact?.firstName?.trimLowerCase() == firstName?.trimLowerCase() &&
                contact?.lastName?.trimLowerCase() == lastName?.trimLowerCase()
    }

    fun compareNameWithSelected(recipient: SelectedAddRecipient?, contact: Contact?): Boolean {
        return contact?.firstName?.trimLowerCase() == (
                if (recipient != null)
                    recipient.firstName?.trimLowerCase()
                else
                    addedRecipient?.firstName?.trimLowerCase())
                &&
                contact?.lastName?.trimLowerCase() == (
                if (recipient != null)
                    recipient.lastName?.trimLowerCase()
                else
                    addedRecipient?.lastName?.trimLowerCase())
    }

    fun isNickNameDuplication(contact: Contact?): Boolean {
        if (isFromRecipientEdit && compareNickNameWithSelected(null, duplicateContact)) {
            return false
        }
        return !contact?.nickName.isNullOrEmpty() && !nickName.isNullOrEmpty() && contact?.nickName?.trimLowerCase() == nickName?.trimLowerCase()
    }

    fun compareNickNameWithSelected(recipient: SelectedAddRecipient?, contact: Contact?): Boolean {
        val nickName =  addedRecipient?.nickName
        if(nickName.isNullOrEmpty() == contact?.nickName.isNullOrEmpty())
            return true
        return !contact?.nickName.isNullOrEmpty() && !nickName.isNullOrEmpty() && contact?.nickName?.trimLowerCase() == nickName.trimLowerCase()
    }

    fun isNickNameNullOrEmpty(contact: Contact?): Boolean {
        return contact?.nickName.isNullOrEmpty() && nickName.isNullOrEmpty()
    }


    fun isPhoneDuplication(contact: Contact?, position: Int): Boolean {
        for (item in contact?.phoneContacts?.phoneToken ?: mutableListOf()) {
            // Duplicate token check should not be done against different recipient type
            if (ContactType.getRecipientTypeFromString(contact?.contactType)
                    .lowercase() != addedRecipient?.recipientType?.value?.lowercase()
            ) {
                return false
            }
            return getCachedMobileNoUiList()[position].value?.phoneDigitOnlyString() == item.phone.phoneDigitOnlyString()
        }
        return false
    }

    fun isEmailDuplication(contact: Contact?, position: Int): Boolean {
        for (item in contact?.emailContacts?.emailToken ?: mutableListOf()) {
            // Duplicate token check should not be done against different recipient type
            if (ContactType.getRecipientTypeFromString(contact?.contactType)
                    .lowercase() != addedRecipient?.recipientType?.value?.lowercase()
            ) {
                return false
            }
            return getCachedEmailUiList()[position].value?.lowercase() == item.emailAddress.lowercase()
        }
        return false
    }


    fun isBankAccountDuplication(contact: Contact?, position: Int): Boolean {
        for (item in contact?.accountContacts?.acctToken ?: mutableListOf()) {
            // Duplicate token check should not be done against different recipient type
            if (ContactType.getRecipientTypeFromString(contact?.contactType)
                    .lowercase() != addedRecipient?.recipientType?.value?.lowercase()
            ) {
                return false
            }
            return (getCachedBankAccUiList()[position].accNo == item.accountNumber
                    && getCachedBankAccUiList()[position].routingNo == item.routingNumber)
        }
        return false
    }


    fun getDuplicateContact(): Contact? {
        return duplicateContact
    }

    fun getCachedMobileNoUiList(): MutableList<AddMobileNoUiData> {
        return cachedMobileNoUiList
    }

    fun addToCachedMobileNoUiList(list: List<AddMobileNoUiData>) {
        cachedMobileNoUiList.addAll(list)
    }

    fun addToCachedMobileNoUiList(data: AddMobileNoUiData) {
        cachedMobileNoUiList.add(data)
    }

    fun removeAllFromCachedMobile() {
        cachedMobileNoUiList.clear()
    }

    fun removeFromCachedMobileNo(position: Int) {
        cachedMobileNoUiList.removeAt(position)
    }

    fun getCachedEmailUiList(): MutableList<AddEmailUiData> {
        return cachedEmailUiList
    }

    fun addToCachedEmailUiList(list: List<AddEmailUiData>) {
        cachedEmailUiList.addAll(list)
    }

    fun addToCachedEmailUiList(data: AddEmailUiData) {
        cachedEmailUiList.add(data)
    }

    fun removeFromCachedEmail(position: Int) {
        cachedEmailUiList.removeAt(position)
    }

    fun removeAllFromCachedEmail() {
        cachedEmailUiList.clear()
    }

    fun getCachedBankAccUiList(): MutableList<AddBankAccUiData> {
        return cachedBankAccUiList
    }

    fun addToCachedBankAccUiList(data: AddBankAccUiData) {
        cachedBankAccUiList.add(data)
    }

    fun setBankAccUiList(data: List<AddBankAccUiData>) {
        cachedBankAccUiList = data as MutableList<AddBankAccUiData>
    }

    fun removeFromCachedBankAcc(position: Int) {
        cachedBankAccUiList.removeAt(position)
    }

    fun updateBankAccTypeFor(position: Int, type: AccountType) {
        cachedBankAccUiList[position].accType = type
    }

    fun getSelectedRecipient(): SelectedAddRecipient {
        if (selectedRecipient == null)
            selectedRecipient = SelectedAddRecipient()
        return selectedRecipient as SelectedAddRecipient
    }

    fun setSelectedRecipient(recipient: SelectedAddRecipient?) {
        //First cleanup existing values
        cleanup()

        //Then set new selected recipient values
        this.selectedRecipient = recipient
    }

    /**
     * Iterate through all the token collection and check each object has some value or not.
     * While counting for tokens, we have to ignore the empty fields.
     */
    fun tokenCount(): Int {
        val mobileCount = cachedMobileNoUiList.filter { !it.value.isNullOrBlank() }.size
        val emailCount = cachedEmailUiList.filter { !it.value.isNullOrBlank() }.size
        val bankCount = cachedBankAccUiList.filter { !it.isNotFilled() }.size
        return mobileCount + emailCount + bankCount
    }

    /**
     * Check if [cachedMobileNoUiList] has at least one data with null or blank value
     */
    fun hasEmptyMobileNo(): Boolean {
        for (data in cachedMobileNoUiList) {
            if (data.value.isNullOrBlank())
                return true
        }
        return false
    }

    /**
     * Check if [cachedEmailUiList] has at least one data with null or blank value
     */
    fun hasEmptyEmail(): Boolean {
        for (data in cachedEmailUiList) {
            if (data.value.isNullOrBlank())
                return true
        }
        return false
    }

    /**
     * Check if [cachedBankAccUiList] has at least one data with incomplete info
     */
    fun hasIncompleteBankDetails(): Boolean {
        for (data in cachedBankAccUiList) {
            if (data.isNotFilled())
                return true
        }
        return false
    }

    fun hasAtLeastOneValidToken(): Boolean {
        for (mobile in cachedMobileNoUiList) {
            if (mobile.isValid)
                return true
        }

        for (email in cachedEmailUiList) {
            if (email.isValid)
                return true
        }

        for (bankAcc in cachedBankAccUiList) {
            if (!bankAcc.isNotFilled())
                return true
        }

        return false
    }

    fun getTokensFromCachedData(): List<SelectedRecipientContact> {
        val contactList = mutableListOf<SelectedRecipientContact>()
        for (item in getCachedMobileNoUiList()) {
            if (!item.value.isNullOrBlank()) {
                contactList.add(
                    SelectedRecipientContact(
                        value = item.value,
                        type = ZelleContactType.PhoneNumber,
                        errorMessage = item.errorMessage,
                        recipientName = item.recipientName
                    )
                )
            }
        }
        for (item in getCachedEmailUiList()) {
            if (!item.value.isNullOrBlank()) {
                contactList.add(
                    SelectedRecipientContact(
                        value = item.value,
                        type = ZelleContactType.EmailAddress,
                        errorMessage = item.errorMessage,
                        recipientName = item.recipientName
                    )
                )
            }
        }
        for (item in getCachedBankAccUiList()) {
            if (!item.isNotFilled()) {
                contactList.add(
                    SelectedRecipientContact(
                        value = item.accNo,
                        type = ZelleContactType.AccountNumber,
                        errorMessage = item.errorMessage,
                        recipientName = item.recipientName
                    )
                )
            }
        }
        return contactList
    }

    fun getSelectedRecipientNameByType(): String? {
        return if (addedRecipient == null)
            selectedRecipient?.getSelectedRecipientNameByType()
        else
            addedRecipient?.getSelectedRecipientNameByType()
    }

    fun setAddRecipientMethod(addRecipientMethod: AddRecipientMethod) {
        this.addRecipientMethod = addRecipientMethod
    }

    fun getAddRecipientMethod(): AddRecipientMethod {
        return this.addRecipientMethod
    }

    fun createRecipientUser(contact: Contact?): RecipientUser {

        val emailContacts = mutableListOf<RecipientContact>()
        val phoneContacts = mutableListOf<RecipientContact>()
        val bankContacts = mutableListOf<RecipientContact>()

        // TODO remove static enrolled value
        if (contact?.phoneContacts?.phoneToken?.isNotEmpty() == true) {
            contact.phoneContacts.phoneToken.forEach { mobileContact ->
                mobileContact.phone.let { value ->
                    val uiMobile = RecipientContact(
                        type = RecipientContactType.PHONE,
                        tokenId = "",
                        value = value,
                        isZelleEnrolled = true
                    )
                    phoneContacts.add(uiMobile)
                }
            }
        }

        if (contact?.emailContacts?.emailToken?.isNotEmpty() == true) {
            contact.emailContacts.emailToken.forEach { emailContact ->
                emailContact.emailAddress.let { value ->
                    val uiEmail = RecipientContact(
                        type = RecipientContactType.EMAIL,
                        tokenId = "",
                        value = value,
                    )
                    emailContacts.add(uiEmail)
                }
            }
        }
        if (contact?.accountContacts?.acctToken?.isNotEmpty() == true) {
            contact.accountContacts?.acctToken!!.forEach { accToken ->
                accToken.accountNumber.let { value ->
                    val uiBank = RecipientContact(
                        type = RecipientContactType.BANK,
                        value = value,
                        tokenId = "",
                        isZelleEnrolled = false
                    )
                    bankContacts.add(uiBank)
                }
            }
        }

        return RecipientUser(
            accountId = contact?.contactId?:"",
            firstName = contact?.firstName,
            lastName = contact?.lastName,
            nickName = contact?.nickName,
            businessName = contact?.businessName ?: "",
            phoneContacts = phoneContacts,
            emailContacts = emailContacts,
            bankContacts = bankContacts,
            isZelleEnrolled = selectedRecipient?.recipientType == SelectedRecipientType.INDIVIDUAL,
            contactType = if (selectedRecipient?.recipientType == SelectedRecipientType.INDIVIDUAL) ContactType.PERSON else ContactType.BUSINESS
        )
    }

    fun cleanup() {
        this.selectedRecipient = null
        this.firstName = null
        this.lastName = null
        this.nickName = null
        this.businessName = null
        this.isFromEdit = null
        this.isFromRecipientEdit = false
        this.firstNameValid = false
        this.lastNameValid = false
        this.businessNameValid = false
        this.addedRecipient = null
        this.tempRecipient = null
        this.addDetailsBankUiData = mutableListOf()
        if (this.cachedMobileNoUiList.isNotEmpty())
            this.cachedMobileNoUiList.clear()
        if (this.cachedEmailUiList.isNotEmpty())
            this.cachedEmailUiList.clear()
        if (this.cachedBankAccUiList.isNotEmpty())
            this.cachedBankAccUiList.clear()
    }
}