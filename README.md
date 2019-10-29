# Simple single-file-based filesystem.

[![Build Status](https://travis-ci.com/beargummy/simple-file-system.svg?branch=master)](https://travis-ci.com/beargummy/simple-file-system)

Simple single-file-backed file system that supports basic file operations 
like create, write content, read content, delete.

There are limited directories support in terms of file placement. 
One may create files under some paths, with `/` as default parts separator.

Default implementation uses single file as underlying block storage.
No directory support at all so far.

## Usage
So far the best approach to grasp idea how to use this FileSystem is DefaultFileSystemTest test class,
which contains basic CRUD scenarios.

## TODOs
- [x] Basic file operations support: create, write, read, delete.
- [x] Concurrency control.
- [x] Multi-block files support.
- [x] Directories support.
- [ ] Fine-grained concurrency control.
