# ROADMAP

v1.0
2020-03-28

- [x] user prefs (home position, has seen onboarding, etc.)
- [x] Notification / Alarme: no sound if i was already doing stuff
- [x] Ensure background location on Pixel 2!!
- [x] background location services
- [x] logo
- [x] fiche PlayStore
- [x] flows d'erreur et de denial de permission - just test it doesn't crash
- [x] Setting button - hide
- [x] Fix coroutine stuff
- [x] Styling (buttons, status bar)
- [x] Welcome message
- [x] Hashtag and copy across the app
- [x] Explain graphically what Confinemap does in a tutorial (why do we need the permission??)
- [x] Move to the bottom of the screen
- [x] Location service doesn't start!!! on restart
- [x] First ever launch: location doesn't get updated
- [x] Not clear when i can move and when i can't
- [x] Position of My Location button: https://stackoverflow.com/questions/36785542/how-to-change-the-position-of-my-location-button-in-google-maps-using-android-st and/or my own stuff to go back home or to me
- [x] Touch area of my settings icon
- [x] if no location services i need to center on default
- [x] Dragging drawer does nothing

v1.1
2020-03-20

- [x] Git, versioning and snapshot: screenshot, screencast, release notes, version
- [x] Choose home flow
- [x] Make MainActivity single instance or whatever
- [x] Proguard
- [x] Bigger buttons
- [x] icon with white background on Pixel 2
- [x] ne pas recentrer sur la maison quand je reviens
- [x] Review my camera position: when do i need to zoom on home, when do i need too zoom on me
- [x] Keep a journal of lessons learnt

v.next - SORTED

- [ ] Bug: airplane mode but with location, I should still be able to use my loc but it doesn't let me. Also, when Airplane mode is turned off I should be recovering
- [x] debug tool: fake gps location from within the app. Lesson learnt: most SO posts are about mocking using LocationManager, but we use Google Play Services Location. It's actually different
- [ ] forbid start balade if not in range
- [ ] alerter when user gets out even with activity open
- [ ] FAB for start / stop
- [ ] Sound/notification when you get back in
- [ ] Instant Apps
- [ ] TIMER in activity and in notif -- (maybe use AlarmManager to schedule the alarm)
- [ ] Customize limites par défaut (1km, 1h)
- [ ] Warning before debut balade si deja au dehors
- [ ] Even when map not fully loaded on load, the demo starts. Explore splash screen instead
- [ ] Fabric integration
- [ ] Test on various screen sizes (esp. map location)
- [ ] Try without GPS
- [x] button to go back home
- [ ] offer a way to recover from no location settings
- [ ] Back button in onboarding
- [ ] Traduction
- [ ] Bug: blue dot not shown if services granted after denial (separately)

