# Contributing

Every contributor agrees to these terms before their first contribution is merged.

## License

This repository is licensed under the GNU Affero General Public License, version 3 or later, with the additional terms in [NOTICE](NOTICE) under section 7 of that license.

When you contribute to this repository, you license your contribution under those same terms. You authorize the NOTICE terms for it under section 7, and you confirm that you have the right to license it this way.

Code you did not write keeps its original copyright and license. Name its source and license when you add it.

## Copyright headers

Files that carry a copyright header use the project's collective notice:

    SPDX-FileCopyrightText: 2026 Reseam contributors <https://reseam.app>
    SPDX-License-Identifier: AGPL-3.0-or-later

The commit history records who wrote what. A file that names another copyright holder falls outside the NOTICE terms, so new files use this header unchanged.

## Translations

Reseam Manager's text is in `composeApp/src/commonMain/composeResources/values/strings.xml`, and the Android notification channels are in `composeApp/src/androidMain/res/values/strings.xml`. Translations come from Weblate as `values-<locale>` folders next to them. Don't edit those by hand, since Weblate overwrites them. Android lists a language in its per-app language setting once `res/values-<locale>` exists, through the generated locale config. Patch names and descriptions come translated from each bundle.
