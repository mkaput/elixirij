defmodule TypespecUnion do
  @type t :: %__MODULE__{
          type: atom() | nil
        }
end
