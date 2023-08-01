<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('totp'); section>
    <#if section="header">
        ${msg("doLogIn")}
    <#elseif section="form">
        <form id="kc-otp-login-form" class="${properties.kcFormClass!}" action="${url.loginAction}"
            method="post">

            <div class="${properties.kcFormGroupClass!}">
                <div class="${properties.kcLabelWrapperClass!}">
                    <label for="otp" class="${properties.kcLabelClass!}">${msg("loginOtpOneTime")}</label>
                </div>

                <div class="${properties.kcInputWrapperClass!}">
                    <input id="otp" name="otp" autocomplete="off" type="text" class="${properties.kcInputClass!}"
                           autofocus aria-invalid="<#if messagesPerField.existsError('totp')>true</#if>"/>

                    <#if messagesPerField.existsError('totp')>
                        <span id="input-error-otp-code" class="${properties.kcInputErrorMessageClass!}"
                              aria-live="polite">
                            ${kcSanitize(messagesPerField.get('totp'))?no_esc}
                        </span>
                    </#if>
                </div>
            </div>

            <div class="${properties.kcFormGroupClass!}">
                <div id="kc-form-options" class="${properties.kcFormOptionsClass!}">
                    <div class="${properties.kcFormOptionsWrapperClass!}" />
                </div>

                <div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
                    <input
                            class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}"
                            name="resendOtp" id="kc-resend-otp" type="submit" value='Resend OTP' />
                </div>

                <div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
                    <input
                        class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}"
                        name="login" id="kc-login" type="submit" value="${msg("doLogIn")}" />
                </div>
            </div>
        </form>
    </#if>
</@layout.registrationLayout>