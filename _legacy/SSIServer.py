#!/usr/bin/env python
'''Run me with `./SSIServer.py 8000`'''

from BaseHTTPServer import HTTPServer
from cStringIO import StringIO
from sys import argv
import os
import re
import SimpleHTTPServer as ss


class SSIHandler(ss.SimpleHTTPRequestHandler):
  directive = r'''(.*?)<!--#(\w+)\s+(\w+)=["'`]([^"'`]+)["'`]\s+-->'''

  def __init__(self, *args, **kwargs):
    ss.SimpleHTTPRequestHandler.__init__(self, *args, **kwargs)
    self.extensions_map['.shtml'] = 'text/html'

  def include_elem(self, out_f, attr, val):
    if attr is 'file':
      try:
        f = open(val)
      except IOError:
        return
      self.copyfile(f, out_f)
    elif attr == 'virtual':
      old_path = self.path
      self.path = val
      self.copyfile(self.send_head(False), out_f)
      self.path = old_path

  def fill_shtml(self, path, f):
    print "filling shtml for", path
    elem_fillers = {
      'include': self.include_elem
      #TODO: other kinds of directives?
    }
    new_f = StringIO()
    for line in f:
      m = re.search(self.directive, line)
      if not m:
        new_f.write(line)
      else:
        print "found directive:", line
        before,element,attr,value = m.groups()
        if element in elem_fillers:
          if before:
            new_f.write(before)
          elem_fillers[element](new_f,attr,value)
        else:
          self.wfile.write(line)
    return new_f

  def send_head(self, send_it=True):
      path = self.translate_path(self.path)
      f = None
      if os.path.isdir(path):
          if not self.path.endswith('/'):
              # redirect browser - doing basically what apache does
              self.send_response(301)
              self.send_header("Location", self.path + "/")
              self.end_headers()
              return None
          for index in "index.html", "index.htm", "index.shtml":
              index = os.path.join(path, index)
              if os.path.exists(index):
                  path = index
                  break
          else:
              return self.list_directory(path)
      ctype = self.guess_type(path)
      # Hack around the strange bug where extensions_map isn't initialized correctly.
      if path.endswith('.shtml'):
        ctype = 'text/html'
      try:
          # Always read in binary mode. Opening files in text mode may cause
          # newline translations, making the actual size of the content
          # transmitted *less* than the content-length!
          f = open(path, 'rb')
      except IOError:
          self.send_error(404, "File not found")
          return None
      if not send_it:
        if path.endswith('.shtml'):
          return self.fill_shtml(path,f)
        return f
      self.send_response(200)
      self.send_header("Content-type", ctype)
      fs = os.fstat(f.fileno())
      if path.endswith('.shtml'):
        f = self.fill_shtml(path, f)
        f.seek(0, os.SEEK_END)
        self.send_header("Content-Length", str(f.tell()))
        f.seek(0)
      else:
        self.send_header("Content-Length", str(fs[6]))
      self.send_header("Last-Modified", self.date_time_string(fs.st_mtime))
      self.end_headers()
      return f

if __name__ == '__main__':
  port = int(argv[1]) if len(argv) > 1 else 8000
  httpd = HTTPServer(('',port), SSIHandler)
  print 'Serving at http://localhost:%d/' % port
  httpd.serve_forever()
