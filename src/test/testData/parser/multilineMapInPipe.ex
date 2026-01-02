# Minimal case: multiline map argument in piped call
%Foo{}
|> Foo.changeset(%{
  foo: 2,
  moo: tags[:x],
  boo: "test@example.com"
})
|> Repo.insert!()
