# Klarblatt design

Klarblatt is an accessibility-first news reader for people who are blind,
losing their sight, or find conventional feed readers difficult to use. It is
not an alternate view of another product. Every request uses this design.

## Design principles

- **Readable before configuration.** Signed-out pages start with large white text
  on black. Text-size controls appear in the header of every page.
- **Topics before technology.** A new reader chooses subjects rather than finding
  RSS or Atom URLs. Adding a website remains available for readers who want it.
- **Structure before volume.** Article headings, list items, quotations, and lead
  sentences form an extractive “main points” section before the full text.
- **Multiple ways to read.** Readers can change theme, type size, line spacing,
  font, and letter spacing, or use browser speech synthesis to listen.
- **Progress stays predictable.** Unread lists are pinned when opened, saving is
  independent of read state, and actions return to a safe, known location.
- **Enhancement stays optional.** Forms and navigation work without JavaScript.
  JavaScript adds read-aloud and in-place saving without replacing core actions.

## Product structure

The first authenticated page is `/topics`. The primary reading routes are:

- `/topics` and `/topics/browse` for followed and available subjects
- `/list` for new or all articles
- `/read/{id}` for an article and its key points
- `/saved` for bookmarks
- `/sources` for individual websites and feeds
- `/display` for visual and reading preferences
- `/settings` for account, newsletter, and optional Kindle delivery settings

Legacy Kindle-oriented GET routes remain in the code for compatibility, but the
MVC interceptor redirects them before users can see those templates:

- `/` → `/topics`
- `/items` → `/list`
- `/articles/{id}` → `/read/{id}`

`EditionResolver` always returns `Edition.ACCESSIBLE`. The interceptor applies
the `accessible/` template prefix and display preferences to every request.
There is no host, query parameter, or cookie edition switch.

## Reading and display

Display preferences are stored in a cookie so login and account-recovery pages
are readable before authentication. They are also stored per account so another
device can inherit them after sign-in.

The read-aloud feature ranks available voices for the page language, remembers
the chosen voice and speed locally, and falls back when an online voice fails.
Article text is split into short utterances to avoid browser speech timeouts.

Key points use only text already present in the article. Klarblatt does not use a
summarising model and does not send article text to an AI service.

## Data and optional features

Accounts, RSS/Atom refresh, article extraction, saved articles, newsletters,
administration, and rate limits use the existing Spring services and PostgreSQL
schema. Send-to-Kindle is optional and appears only when an account has a Kindle
address.

Klarblatt runs on one application domain. A separate static marketing site may
use another domain, but it is not another edition of the reader. Extrablatt is a
separate sibling product for Kindle-first reading:
<https://reader.extrablatt.app>.
