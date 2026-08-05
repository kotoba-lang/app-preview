# app-preview

**Preview, on [`mokuroku`](https://github.com/kotoba-lang/mokuroku).**

Design: [ADR-2608035000](https://github.com/com-junkawasaki/root/blob/main/90-docs/adr/2608035000-app-standard-application-suite-on-a-shared-catalog-kernel.edn).

Capability: `fs/browse`. Nothing in this repo performs the effect — the host
supplies the provider function, and that is where the grant is spent.

**The selected page is drawn**, by `kotoba-lang/hanmen`. A host that has
spent the grant hands over page values — a size, a rotation, placed marks —
and `page/render` puts the selected one above the listing:

```clojure
(page/render cat {:pages {0 page-value} :image-href (fn [{:keys [index]}] …)})
```

Decoding is still nobody's job here. Without `:pages` this is the listing it
always was, and without `:image-href` the drawn page loads nothing at all —
a host that has not decided its CSP is not forced to.

## Three decisions

**Identity is the page index, not the printed label.** A document with front
matter has two pages both labelled `i`, and a roman-numbered preface restarts
at 1.

**Rotation is applied before the size is reported.** A PDF page carries a
rotation independent of its media box; reporting the unrotated box shows a
landscape page as portrait and every thumbnail grid lays it out wrongly.

**Scanned pages are reported, not hidden.** A page with no extractable text is
why a search over the document comes back empty — without saying so, the user
concludes the search is broken. `text-coverage` says how much of the document
a search can actually see, and the empty state names the cause.

`source/undecodable` is a state distinct from a refused grant: the fix is a
decoder, not a permission, and telling the user to check permissions when the
format is simply unsupported sends them somewhere useless. Decoding itself is
`kotoba-lang/kasane`'s job.

## Test

```sh
clojure -M:local:test    # sibling checkouts
clojure -M:test          # pinned git deps
clojure -M:lint
```

design-quality: 100.00 on every window state including awaiting-grant
(2026-08-03) and with a page drawn (2026-08-05).
