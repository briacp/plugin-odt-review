# ODT Review

This plugin introduce an option to import and export `.ODT` review files. 

## Exporting a review file

When exporting a translation to be reviewed, a dialog box is displayed where you can choose where to save the review file and which file you want to include in the review. The default name of the review file is `[project_name]_[source]-[target]_review.odt`.

![image](https://github.com/briacp/plugin-odt-review/assets/4170697/d127bdc2-1fc8-46a1-bde4-155cad0e5150)

## Importing a review file

Once the review process is done, you can import the ODT file back in the project. If the reviewed translation differs from the current translation, it's replaced by the review. If there's a reviewer note, it's appended in the Notes panel.

## Reviewing a project

![image](https://github.com/briacp/plugin-odt-review/assets/4170697/ce004dee-7ca3-43d6-96a0-fe541b37116e)

The review file is comprised of several tables, one for each source file. Each table has three columns: the source text (protected and not editable), the translated text (editable) and the review notes.

## Sponsor

Thanks a lot to the [Translation Studies Program](https://www.csulb.edu/clorinda-donato-center/programs) and the [Donato Center](https://www.csulb.edu/clorinda-donato-center) at [California State University, Long Beach](https://www.csulb.edu/) (CSULB) for sponsoring the development of this plugin.

![CSULB](https://www.csulb.edu/themes/custom/csulb/images/lb.svg)

## License

This project is distributed under the GNU general public license version 3 or later.

## Installation

You can get a plugin jar file from zip distribution file.
OmegaT plugin should be placed in `$HOME/.omegat/plugins` or `%APPDATA%\OmegaT\plugins`
depending on your operating system.

## License

This project is distributed under the GNU general public license version 3 or later.
