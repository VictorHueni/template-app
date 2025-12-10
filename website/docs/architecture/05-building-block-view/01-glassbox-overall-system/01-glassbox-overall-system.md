# Glassbox Overall System

Here you describe the decomposition of the overall system using the following
glass box template. It contains

- an overview diagram

- a motivation for the decomposition

- black box descriptions of the contained building blocks. For these we offer
  you alternatives:

  - use _one_ table for a short and pragmatic overview of all contained building
    blocks and their interfaces

  - use a list of black box descriptions of the building blocks according to the
    black box template (see below). Depending on your choice of tool this list
    could be sub-chapters (in text files), sub-pages (in a Wiki) or nested
    elements (in a modeling tool).

- (optional:) important interfaces, that are not explained in the black box
  templates of a building block, but are very important for understanding the
  glass box. Since there are so many ways to specify interfaces why do not
  provide a specific template for them. In the worst case you have to specify
  and describe syntax, semantics, protocols, error handling, restrictions,
  versions, qualities, necessary compatibilities and many things more. In the
  best case you will get away with examples or simple signatures.

**_&lt;Overview Diagram&gt;_**

Motivation

: _&lt;text explanation&gt;_

Contained Building Blocks

: _&lt;Description of contained building block (black boxes)&gt;_

Important Interfaces

: _&lt;Description of important interfaces&gt;_

Insert your explanations of black boxes from level 1:

If you use tabular form you will only describe your black boxes with name and
responsibility according to the following schema:

| **Name**    | **Responsibility** |
| ----------- | ------------------ |
| Black Box 1 |  *&lt;Text&gt;*    |
| Black Box 2 |  *&lt;Text&gt;*    |

If you use a list of black box descriptions then you fill in a separate black
box template for every important building block . Its headline is the name of
the black box.

### &lt;Name black box 1&gt;

Here you describe &lt;black box 1&gt; according the the following black box
template:

- Purpose/Responsibility

- Interface(s), when they are not extracted as separate paragraphs. This
  interfaces may include qualities and performance characteristics.

- (Optional) Quality-/Performance characteristics of the black box,
  e.g.availability, run time behavior, ….

- (Optional) directory/file location

- (Optional) Fulfilled requirements (if you need traceability to requirements).

- (Optional) Open issues/problems/risks

_&lt;Purpose/Responsibility&gt;_

_&lt;Interface(s)&gt;_

_&lt;(Optional) Quality/Performance Characteristics&gt;_

_&lt;(Optional) Directory/File Location&gt;_

_&lt;(Optional) Fulfilled Requirements&gt;_

_&lt;(optional) Open Issues/Problems/Risks&gt;_

### &lt;Name black box 2&gt;

_&lt;black box template&gt;_

### &lt;Name black box n&gt;

_&lt;black box template&gt;_

### &lt;Name interface 1&gt;

…

### &lt;Name interface m&gt;
