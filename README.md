# DoctorDial
An Android app to repeatedly dial a number until it is answered

An app written to test whether it is easier to learn the Android API and build an automated caller than to get an appointment at a GP in Stratford.  Turns out writing the app was indeed easier, but getting it to the point where it was user-friendly and shippable to the Android Play store was not.

The app doesn't work super consistently across different Android versions as it relies on some slightly hacky workarounds to get around the fact that Android doesn't let you listen in to calls in progress.  Specifically, the app starts the call, and then waits for a configurable delay before checking the pickup state.  If the call has been picked up, it goes to loudspeaker.
